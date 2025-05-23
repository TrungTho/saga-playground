package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.TestConstants;
import com.saga.playground.checkoutservice.basetest.PostgresContainerBaseTest;
import com.saga.playground.checkoutservice.basetest.ZookeeperTestConfig;
import com.saga.playground.checkoutservice.configs.CuratorConfig;
import com.saga.playground.checkoutservice.configs.ObjectMapperConfig;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.GRPCConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.grpc.services.OrderGRPCService;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutHelper;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import com.saga.playground.checkoutservice.workers.workerregistration.CheckoutRegistrationWorker;
import com.saga.playground.checkoutservice.workers.workerregistration.ZookeeperWorkerRegistration;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.SneakyThrows;
import org.apache.curator.test.TestingServer;
import org.apache.logging.log4j.util.Strings;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import static java.lang.System.exit;

/**
 * Because the logic is a bit complicated and involve multiple components
 * Therefore we will need to have integration test for the happy case in order to ensure
 * that the main flow works as expected
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
    OrderGRPCService.class,
    CheckoutHelper.class,
    ObjectMapperConfig.class
})
@EnableRetry
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CheckoutProcessingWorkerIntegrationTest extends PostgresContainerBaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoSpyBean
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @MockitoSpyBean
    private ZookeeperDistributedLock distributedLock;

    @MockitoSpyBean
    private OrderGRPCService orderGRPCService;

    @MockitoSpyBean
    private CheckoutHelper checkoutHelper;

    @Autowired
    private CheckoutRepository checkoutRepository;

    @MockitoSpyBean
    private CheckoutRegistrationWorker registrationWorker;

    @Autowired
    private TestingServer testingServer;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoSpyBean
    private CheckoutProcessingWorker checkoutProcessingWorker;

    @Autowired
    private Environment environment;

    @BeforeAll
    void check() {
        Assertions.assertNotNull(testingServer);
    }

    @AfterAll
    void shutdownTestingServer() throws IOException {
        testingServer.stop();
        testingServer.close();
    }

    @AfterEach
    void reset() throws Exception {
        LogManager.getLogManager().readConfiguration();
    }

    @Nested
    class TestProcessCheckout {
        @BeforeEach
        void verifyEmptyData() {
            var inboxes = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(inboxes.isEmpty());
        }

        private void verifyRetry(CapturedOutput output) {
            Assertions.assertTrue(output.toString().contains(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED));
            for (int i = 0; i < WorkerConstant.MAX_RETRY_TIMES; i++) {
                Assertions.assertTrue(output.toString().contains(", retry %d".formatted(i)),
                    "Retry number %d should be printed".formatted(i));
            }
        }

        private void verifyNoRetry(CapturedOutput output) {
            Assertions.assertTrue(
                output.toString().contains(", retry %d".formatted(0)));
            for (int i = 1; i < WorkerConstant.MAX_RETRY_TIMES; i++) {
                Assertions.assertFalse(output.toString().contains(", retry %d".formatted(i)),
                    "Retry number %d should be printed".formatted(i));
            }
        }

        private void verifyFailedInboxUpdate(String orderId) {
            // verify inbox is saved with FAILED and note
            var inbox = transactionalInboxOrderRepository.findByOrderId(orderId);
            Assertions.assertFalse(inbox.isEmpty());
            Assertions.assertEquals(InboxOrderStatus.FAILED, inbox.get().getStatus());
            Assertions.assertNotNull(inbox.get().getNote());
        }

        @Test
        void RetryAndRecover(CapturedOutput output) {
            Mockito.doThrow(new RuntimeException())
                .when(orderGRPCService).switchOrderStatus(1);

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("1"));

            Assertions.assertTrue(
                output.toString().contains(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED));
            verifyRetry(output);
        }

        @Test
        void InvalidNumberFormat(CapturedOutput output) {
            String orderId = "1";
            var mockInbox = Instancio.of(TransactionalInboxOrder.class)
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .set(Select.field(TransactionalInboxOrder::getOrderId), orderId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId))
                .ignore(Select.field(TransactionalInboxOrder::getNote))
                .ignore(Select.field(TransactionalInboxOrder::getId))
                .create();
            Assertions.assertNull(mockInbox.getId());

            transactionalInboxOrderRepository.save(mockInbox);

            Assertions.assertNull(mockInbox.getNote());

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("dummy")
            );

            Assertions.assertTrue(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
                .findByOrderId("dummy");

            verifyNoRetry(output);
        }

        @Test
        void InvalidAction(CapturedOutput output) {
            String orderId = "1";
            var mockInbox = Instancio.of(TransactionalInboxOrder.class)
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .set(Select.field(TransactionalInboxOrder::getOrderId), orderId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId))
                .ignore(Select.field(TransactionalInboxOrder::getNote))
                .ignore(Select.field(TransactionalInboxOrder::getId))
                .create();
            Assertions.assertNull(mockInbox.getId());

            transactionalInboxOrderRepository.save(mockInbox);

            Assertions.assertNull(mockInbox.getNote());

            Mockito.doThrow(new StatusRuntimeException(Status.UNKNOWN.withDescription(
                    GRPCConstant.ORDER_SERVER_INVALID_ACTION)))
                .when(orderGRPCService).switchOrderStatus(Integer.parseInt(orderId));
            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout(orderId)
            );

            Assertions.assertTrue(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            verifyFailedInboxUpdate(orderId);

            verifyNoRetry(output);
        }

        @Test
        void UnhandledGRPCError(CapturedOutput output) {
            String orderId = "1";
            var mockInbox = Instancio.of(TransactionalInboxOrder.class)
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .set(Select.field(TransactionalInboxOrder::getOrderId), orderId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId))
                .ignore(Select.field(TransactionalInboxOrder::getNote))
                .ignore(Select.field(TransactionalInboxOrder::getId))
                .create();
            Assertions.assertNull(mockInbox.getId());

            transactionalInboxOrderRepository.save(mockInbox);

            Assertions.assertNull(mockInbox.getNote());

            Mockito.doThrow(new StatusRuntimeException(Status.UNKNOWN))
                .when(orderGRPCService).switchOrderStatus(1);
            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("1")
            );

            Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
                .findByOrderId(Mockito.any());
            Assertions.assertEquals(InboxOrderStatus.FAILED, mockInbox.getStatus());
            Assertions.assertNotNull(mockInbox.getNote());
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            verifyFailedInboxUpdate(orderId);

            verifyRetry(output);
        }

        @Test
        void NotFoundInbox(CapturedOutput output) {
            Mockito.doNothing().when(orderGRPCService).switchOrderStatus(1);
            Mockito.when(transactionalInboxOrderRepository.findByOrderId(Mockito.any()))
                .thenReturn(Optional.empty());
            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("1")
            );

            Mockito.verify(checkoutHelper, Mockito.times(0))
                .upsertCheckoutInfo(Mockito.any());
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertTrue(output.toString().contains("INBOX NOT FOUND"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            verifyNoRetry(output);
        }

        @SneakyThrows
        @Test
        void FailedBuildCheckoutInfo(CapturedOutput output) {
            String orderId = "1";
            var mockInbox = Instancio.of(TransactionalInboxOrder.class)
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .set(Select.field(TransactionalInboxOrder::getOrderId), orderId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId))
                .ignore(Select.field(TransactionalInboxOrder::getNote))
                .ignore(Select.field(TransactionalInboxOrder::getId))
                .create();
            Assertions.assertNull(mockInbox.getId());

            transactionalInboxOrderRepository.save(mockInbox);

            Assertions.assertNull(mockInbox.getNote());

            Mockito.doNothing().when(orderGRPCService).switchOrderStatus(Integer.parseInt(orderId));
            Mockito.doReturn(Optional.empty()).when(checkoutHelper)
                .upsertCheckoutInfo(mockInbox);

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("1")
            );

            Mockito.verify(checkoutHelper, Mockito.times(0))
                .postCheckoutProcess(Mockito.any());
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("INBOX NOT FOUND"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            verifyFailedInboxUpdate(orderId);

            verifyNoRetry(output);
        }

        @SneakyThrows
        @Test
        void PaymentGatewayCheckoutFailed(CapturedOutput output) {
            String orderId = "1";
            var mockInbox = Instancio.of(TransactionalInboxOrder.class)
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .set(Select.field(TransactionalInboxOrder::getOrderId), orderId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId))
                .ignore(Select.field(TransactionalInboxOrder::getNote))
                .ignore(Select.field(TransactionalInboxOrder::getId))
                .create();
            Assertions.assertNull(mockInbox.getId());

            transactionalInboxOrderRepository.save(mockInbox);

            Assertions.assertNull(mockInbox.getNote());

            Checkout mockCheckout = Instancio.of(Checkout.class).create();

            Mockito.doNothing().when(orderGRPCService).switchOrderStatus(Integer.parseInt(orderId));
            Mockito.doReturn(Optional.of(mockCheckout)).when(checkoutHelper)
                .upsertCheckoutInfo(mockInbox);
            Mockito.doThrow(new RuntimeException("SOME_ERROR")).when(checkoutHelper)
                .registerCheckout(mockCheckout);

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("1")
            );

            Mockito.verify(checkoutHelper, Mockito.times(0))
                .postCheckoutProcess(Mockito.any());
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("INBOX NOT FOUND"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            verifyFailedInboxUpdate(orderId);

            verifyRetry(output);
        }

        @SneakyThrows
        @Test
        void Successfully(CapturedOutput output) {
            var inboxes = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(inboxes.isEmpty(),
                "Table inbox should not have any record before starting the test");
            var checkouts = checkoutRepository.findAll();
            Assertions.assertTrue(checkouts.isEmpty(),
                "Table checkout should not have any record before starting the test");

            int orderId = 1;
            TransactionalInboxOrder mockInbox =
                new TransactionalInboxOrder("%s".formatted(orderId), TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId));
            transactionalInboxOrderRepository.save(mockInbox);

            Mockito.doNothing().when(orderGRPCService).switchOrderStatus(orderId);

            String mockCheckoutSessionId = UUID.randomUUID().toString();
            Mockito.doReturn(mockCheckoutSessionId).when(checkoutHelper)
                .registerCheckout(Mockito.any());

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("%d".formatted(orderId))
            );

            // assert all records are update
            var inbox = transactionalInboxOrderRepository.findByOrderId("%d".formatted(orderId));
            Assertions.assertTrue(inbox.isPresent(), "Inbox should be persisted");
            Assertions.assertEquals(InboxOrderStatus.DONE, inbox.get().getStatus(),
                "Inbox status should be DONE");

            checkouts = checkoutRepository.findAll();
            Assertions.assertFalse(checkouts.isEmpty(), "Checkout table should not be null after processing");
            Assertions.assertEquals(1, checkouts.size(),
                "Checkout table should contain only 1 record");
            Assertions.assertEquals(PaymentStatus.PROCESSING, checkouts.get(0).getCheckoutStatus(),
                "Checkout status should be %s".formatted(PaymentStatus.PROCESSING));
            Assertions.assertEquals(mockCheckoutSessionId, checkouts.get(0).getCheckoutSessionId(),
                "Checkout session id should match");

            Mockito.verify(checkoutHelper, Mockito.times(1))
                .postCheckoutProcess(Mockito.any());
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("INBOX NOT FOUND"));
            Assertions.assertTrue(output.toString().contains("Successfully submit checkout request for order"));
            verifyNoRetry(output);
        }

        @SneakyThrows
        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void TransactionRollback(CapturedOutput output) {
            var inboxes = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(inboxes.isEmpty(),
                "Table inbox should not have any record before starting the test");
            var checkouts = checkoutRepository.findAll();
            Assertions.assertTrue(checkouts.isEmpty(),
                "Table checkout should not have any record before starting the test");

            int orderId = 1;
            TransactionalInboxOrder mockInbox =
                new TransactionalInboxOrder("%s".formatted(orderId), TestConstants.MOCK_CDC_PAYLOAD.formatted(orderId));
            transactionalInboxOrderRepository.save(mockInbox);

            Checkout mockCheckout = Instancio.of(Checkout.class)
                .ignore(Select.field(Checkout::getId))
                .ignore(Select.field(Checkout::getEventPublished))
                .ignore(Select.field(Checkout::getWebhookPayload))
                .ignore(Select.field(Checkout::getCreatedAt))
                .ignore(Select.field(Checkout::getUpdatedAt))
                .set(Select.field(Checkout::getCheckoutStatus), PaymentStatus.INIT)
                .create();

            var spyCheckout = Mockito.spy(mockCheckout);
            // crash at the end of the method should roll back all DB change
            Mockito.doThrow(new RuntimeException()).when(spyCheckout)
                .setCheckoutStatus(Mockito.any());

            Mockito.doNothing().when(orderGRPCService).switchOrderStatus(orderId);
            Mockito.doReturn(Optional.of(spyCheckout)).when(checkoutHelper)
                .upsertCheckoutInfo(Mockito.any());

            String mockCheckoutSessionId = UUID.randomUUID().toString();
            Mockito.doReturn(mockCheckoutSessionId).when(checkoutHelper)
                .registerCheckout(spyCheckout);

            Assertions.assertDoesNotThrow(
                () -> checkoutProcessingWorker.processCheckout("%d".formatted(orderId))
            );

            // assert all records are update
            var inbox = transactionalInboxOrderRepository.findByOrderId("%d".formatted(orderId));
            Assertions.assertTrue(inbox.isPresent(), "Inbox should be persisted");
            Assertions.assertNotEquals(InboxOrderStatus.DONE, inbox.get().getStatus(),
                "Inbox status should NOT be DONE");

            checkouts = checkoutRepository.findAll();
            Assertions.assertTrue(checkouts.isEmpty(), "Checkout table SHOULD BE EMPTY");
            Assertions.assertFalse(output.toString().contains("INVALID ACTION"));
            Assertions.assertFalse(output.toString().contains("INVALID ORDER FORMAT"));
            Assertions.assertFalse(output.toString().contains("INBOX NOT FOUND"));
            Assertions.assertFalse(output.toString().contains("Successfully submit checkout request for order"));

            transactionalInboxOrderRepository.delete(mockInbox);
            verifyRetry(output);
        }

    }

    @Nested
    @Transactional
    class TestRetrieveExistingOrder {

        @Test
        @SneakyThrows
        void Successfully(CapturedOutput output) {
            String workerId = registrationWorker.getWorkerId();
            Assertions.assertFalse(workerId.isBlank());

            // check no data in DB
            var res = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(res.isEmpty(), "DB should be empty before test start");

            // populate new data in DB
            int numberOfRecords = 10;
            var mockCheckout = Instancio.of(Checkout.class).create();

            var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
                .size(numberOfRecords)
                .ignore(Select.field(TransactionalInboxOrder::getId)) // new record
                .set(Select.field(TransactionalInboxOrder::getWorkerId), workerId)
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    objectMapper.writeValueAsString(mockCheckout))
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.IN_PROGRESS)
                .create();

            transactionalInboxOrderRepository.saveAll(mockOrders);

            var orders = Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullOrders());

            orders.forEach(order -> {
                Assertions.assertEquals(InboxOrderStatus.IN_PROGRESS, order.getStatus());
                Assertions.assertTrue(mockOrders.stream()
                    .anyMatch(item -> item.getOrderId().equals(order.getOrderId())));
            });
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

    @Nested
    // !!! IMPORTANT: don't run this again a real DB
    // because this NOT_SUPPORTED will directly commit to DB without DataJPATest rollback help after test finish
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class TestPullNewOrder {
        @AfterEach
        void clearDB() {
            String jdbcUrl = environment.getProperty("spring.datasource.url");
            Assertions.assertNotNull(jdbcUrl);
            Assertions.assertTrue(Strings.isNotBlank(jdbcUrl));
            if (!jdbcUrl.contains("localhost")) {
                System.out.println("!!!PLEASE USE LOCALHOST DB!!!");
                exit(1);
            }
            transactionalInboxOrderRepository.deleteAll();
        }

        @SneakyThrows
        @Test
        void Successfully(CapturedOutput output) {
            String workerId = registrationWorker.getWorkerId();
            Assertions.assertFalse(workerId.isBlank());

            // check no data in DB
            var res = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(res.isEmpty(), "DB should be empty before test start");

            // populate new data in DB
            int numberOfRecords = 10;
            var mockCheckout = Instancio.of(Checkout.class).create();

            var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
                .size(numberOfRecords)
                .ignore(Select.field(TransactionalInboxOrder::getId)) // new record
                .ignore(Select.field(TransactionalInboxOrder::getWorkerId)) // no worker pick up them yet
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    objectMapper.writeValueAsString(mockCheckout))
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .create();

            transactionalInboxOrderRepository.saveAll(mockOrders);

            mockOrders.forEach(order -> {
                Assertions.assertNotNull(order.getId());
                Assertions.assertEquals(InboxOrderStatus.NEW, order.getStatus());
                Assertions.assertNull(order.getWorkerId());
            });

            var orders = Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullOrders());

            orders.forEach(order -> {
                Assertions.assertEquals(
                    InboxOrderStatus.IN_PROGRESS, order.getStatus(),
                    "order status should be NEW %s".formatted(order));
                Assertions.assertTrue(Strings.isNotBlank(order.getWorkerId()));
                Assertions.assertTrue(mockOrders.stream()
                    .anyMatch(item -> item.getOrderId().equals(order.getOrderId())));
            });

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

        @SneakyThrows
        @Test
        void TransactionRollback(CapturedOutput output) {
            String workerId = registrationWorker.getWorkerId();
            Assertions.assertFalse(workerId.isBlank());

            // check no data in DB
            var res = transactionalInboxOrderRepository.findAll();
            Assertions.assertTrue(res.isEmpty(), "DB should be empty before test start");

            // populate new data in DB
            int numberOfRecords = 10;
            var mockCheckout = Instancio.of(Checkout.class).create();

            var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
                .size(numberOfRecords)
                .ignore(Select.field(TransactionalInboxOrder::getId)) // new record
                .ignore(Select.field(TransactionalInboxOrder::getWorkerId)) // no worker pick up them yet
                .set(Select.field(TransactionalInboxOrder::getPayload),
                    objectMapper.writeValueAsString(mockCheckout))
                .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
                .create();

            transactionalInboxOrderRepository.saveAll(mockOrders);

            // intentionally crash to test transaction rollback
            Mockito.doThrow(new RuntimeException()).when(transactionalInboxOrderRepository).saveAll(Mockito.any());

            // This will throw exception and @Transactional should roll back the IN_PROGRESS status
            Assertions.assertThrows(RuntimeException.class,
                () -> checkoutProcessingWorker.pullOrders());

            mockOrders = transactionalInboxOrderRepository.findAll();
            mockOrders.forEach(order -> Assertions.assertEquals(
                InboxOrderStatus.NEW, order.getStatus(), "order status should be NEW"));

            // verify output & db records
            Assertions.assertTrue(output.toString().contains(
                "Unhandled error when %s pulling new order".formatted(workerId)));
            Assertions.assertTrue(output.toString().contains("%s start querying existing record"
                .formatted(workerId)));
            Assertions.assertFalse(output.toString().contains("found existing orders"));
            Assertions.assertTrue(output.toString().contains("%s acquired lock successfully, start querying new orders"
                .formatted(workerId)));
            Assertions.assertFalse(output.toString().contains("successfully acquired orders"));
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

    }

}
