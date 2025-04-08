package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.basetest.PostgresContainerBaseTest;
import com.saga.playground.checkoutservice.basetest.ZookeeperTestConfig;
import com.saga.playground.checkoutservice.configs.CuratorConfig;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

/**
 * Because the logic is a bit complicated and involve multiple components
 * Therefore we will need to have integration test for the happy case in order to ensure
 * that the main flow works as expected
 * <p>
 * We already test the persistent layer in TransactionalInboxOrderRepositoryTest with the actual Postgres container
 * Therefore we just use H2 DB here for faster bootstrapping
 */
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@TestPropertySource(properties = {
    "zookeeper.port=22181",
    "zookeeper.host=localhost"
})
@DataJpaTest
@Import({
    ZookeeperTestConfig.class,
    CuratorConfig.class,
    ZookeeperDistributedLock.class,
    ZookeeperWorkerRegistration.class,
    CheckoutProcessingWorker.class,
})
class CheckoutProcessingWorkerIntegrationTest extends PostgresContainerBaseTest {
    @Autowired
    private CheckoutProcessingWorker checkoutProcessingWorker;

    @MockitoSpyBean
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @MockitoSpyBean
    private ZookeeperDistributedLock distributedLock;

    @Autowired
    private CheckoutRegistrationWorker registrationWorker;

    @Test
    void testNewOrder_OK(CapturedOutput output) throws JsonProcessingException {
        String workerId = registrationWorker.getWorkerId();
        Assertions.assertFalse(workerId.isBlank());

        // check no data in DB
        var res = transactionalInboxOrderRepository.findAll();
        Assertions.assertTrue(res.isEmpty(), "DB should be empty before test start");

        // populate new data in DB
        int numberOfRecords = 10;
        var mockCheckout = Instancio.of(Checkout.class).create();
        ObjectMapper objectMapper = new ObjectMapper();

        var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .ignore(Select.field(TransactionalInboxOrder::getId)) // new record
            .ignore(Select.field(TransactionalInboxOrder::getWorkerId)) // no worker pick up them yet
            .set(Select.field(TransactionalInboxOrder::getPayload),
                objectMapper.writeValueAsString(mockCheckout))
            .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
            .create();

        transactionalInboxOrderRepository.saveAll(mockOrders);

        // acquire lock & set data as acquired

        Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullNewOrder());

        // verify output & db records
        Assertions.assertTrue(output.toString().contains("%s start querying existing record"
            .formatted(workerId)));
        Assertions.assertFalse(output.toString().contains("found existing orders"));
        Assertions.assertTrue(output.toString().contains("%s acquired lock successfully, start querying new orders"
            .formatted(workerId)));
        Assertions.assertTrue(output.toString().contains("%s successfully acquired orders %s"
            .formatted(workerId, mockOrders.stream().map(TransactionalInboxOrder::getOrderId).toList())));
        Assertions.assertTrue(output.toString().contains("successfully released lock"));

        Mockito.verify(distributedLock, Mockito.times(1))
            .acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS);
        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
            .findNewOrders();
        Mockito.verify(distributedLock, Mockito.times(1))
            .releaseLock(WorkerConstant.WORKER_PULL_ORDER_LOCK);
    }

    @Test
    void testExistingOrder_OK(CapturedOutput output) throws JsonProcessingException {
        String workerId = registrationWorker.getWorkerId();
        Assertions.assertFalse(workerId.isBlank());

        // check no data in DB
        var res = transactionalInboxOrderRepository.findAll();
        Assertions.assertTrue(res.isEmpty(), "DB should be empty before test start");

        // populate new data in DB
        int numberOfRecords = 10;
        var mockCheckout = Instancio.of(Checkout.class).create();
        ObjectMapper objectMapper = new ObjectMapper();

        var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .ignore(Select.field(TransactionalInboxOrder::getId)) // new record
            .set(Select.field(TransactionalInboxOrder::getWorkerId), workerId)
            .set(Select.field(TransactionalInboxOrder::getPayload),
                objectMapper.writeValueAsString(mockCheckout))
            .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.IN_PROGRESS)
            .create();

        transactionalInboxOrderRepository.saveAll(mockOrders);

        // acquire lock & set data as acquired

        Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullNewOrder());

        // verify output & db records
        Assertions.assertTrue(output.toString().contains("%s start querying existing record"
            .formatted(workerId)));
        Assertions.assertTrue(output.toString().contains("found existing orders"));
        Assertions.assertFalse(output.toString().contains("%s acquired lock successfully, start querying new orders"
            .formatted(workerId)));
        Assertions.assertFalse(output.toString().contains("%s successfully acquired orders %s"
            .formatted(workerId, mockOrders.stream().map(TransactionalInboxOrder::getOrderId).toList())));
        Assertions.assertFalse(output.toString().contains("successfully released lock"));

        Mockito.verify(distributedLock, Mockito.times(0))
            .acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS);
        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .findNewOrders();
        Mockito.verify(distributedLock, Mockito.times(0))
            .releaseLock(WorkerConstant.WORKER_PULL_ORDER_LOCK);
    }
}
