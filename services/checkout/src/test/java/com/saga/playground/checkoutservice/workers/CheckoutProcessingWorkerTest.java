package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import com.saga.playground.checkoutservice.workers.workerregistration.CheckoutRegistrationWorker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.stream.Stream;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CheckoutProcessingWorkerTest {

    private final String mockWorkerId = "worker-1";
    @Mock
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @Mock
    private CheckoutRegistrationWorker checkoutRegistrationWorker;

    @Mock
    private ZookeeperDistributedLock distributedLock;

    @InjectMocks
    private CheckoutProcessingWorker checkoutProcessingWorker;

    static Stream<Arguments> generateData() {
        return Stream.of(
            Arguments.of(Collections.emptyList()),
            Arguments.of(Instancio
                .ofList(TransactionalInboxOrder.class).size(10).create())
        );
    }

    @AfterEach
    void reset() throws Exception {
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    void testProcessCheckout_Retry() {
        verifyRetry();
    }

    @Test
    void testProcessCheckout_InvalidOrderId() {
        verifyNoRetry();
    }

    @Test
    void testProcessCheckout_InvalidAction() {
        // make sure try doesn't work
        verifyNoRetry();
    }

    private void verifyNoRetry() {

    }

    private void verifyRetry() {

    }

    @Test
    void testProcessCheckout_NoOrderInbox() {
        verifyNoRetry();
    }

    @Test
    void testProcessCheckout_FailedBuildCheckoutInfo() {
        verifyRetry();
    }

    @Test
    void testProcessCheckout_FailedCheckout() {
        verifyRetry();
    }

    @Test
    void testProcessCheckout_OK() {

    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("generateData")
    void testRetrieveExistingOrder(List<TransactionalInboxOrder> orderList) {
        Mockito.when(checkoutRegistrationWorker.getWorkerId())
            .thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(orderList);

        var res = checkoutProcessingWorker.retrieveExistingOrder();

        Assertions.assertIterableEquals(orderList, res);
    }

    @Test
    void testPullNewOrder_ExistingOrder(CapturedOutput output) throws JsonProcessingException, InterruptedException {
        int numberOfRecords = 10;
        List<TransactionalInboxOrder> mockExisitingOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .set(Select.field(TransactionalInboxOrder::getWorkerId), mockWorkerId)
            .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.IN_PROGRESS)
            .create();

        Mockito.when(checkoutRegistrationWorker.getWorkerId()).thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(mockExisitingOrders);

        checkoutProcessingWorker.pullNewOrder();

        Assertions.assertTrue(output.toString().contains(
            "%s found existing orders: %s".formatted(mockWorkerId,
                mockExisitingOrders.stream().map(TransactionalInboxOrder::getOrderId).toList())
        ));
    }

    @Test
    void testPullNewOrder_AcquireLockFailed(CapturedOutput output) throws JsonProcessingException, InterruptedException {
        Mockito.when(checkoutRegistrationWorker.getWorkerId()).thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(Collections.emptyList());
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(false);

        checkoutProcessingWorker.pullNewOrder();

        Assertions.assertFalse(output.toString().contains("found existing orders"));
        Assertions.assertTrue(output.toString().contains(
            "%s cannot acquire lock to pull new orders".formatted(mockWorkerId)
        ));
        Mockito.verify(distributedLock, Mockito.times(0)).releaseLock(Mockito.any());
    }

    @Test
    void testPullNewOrder_ReleaseWhenExceptionThrown(CapturedOutput output) {
        Mockito.when(checkoutRegistrationWorker.getWorkerId()).thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(Collections.emptyList());
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(true);
        Mockito.when(transactionalInboxOrderRepository.findNewOrders())
            .thenThrow(new RuntimeException());

        Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullNewOrder());

        Assertions.assertFalse(output.toString().contains("found existing orders"));
        Assertions.assertTrue(output.toString().contains("Unhandled error"));
        Mockito.verify(distributedLock, Mockito.times(1)).releaseLock(Mockito.any());
    }

    @Test
    void testPullNewOrder_SuccessfullyPullNewOrder(CapturedOutput output) {
        int numberOfRecords = 10;
        List<TransactionalInboxOrder> mockNewOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .set(Select.field(TransactionalInboxOrder::getWorkerId), mockWorkerId)
            .set(Select.field(TransactionalInboxOrder::getStatus), InboxOrderStatus.NEW)
            .create();

        Mockito.when(checkoutRegistrationWorker.getWorkerId()).thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(Collections.emptyList());
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(true);
        Mockito.when(transactionalInboxOrderRepository.findNewOrders())
            .thenReturn(mockNewOrders);

        ArgumentCaptor<List<TransactionalInboxOrder>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullNewOrder());

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(1))
            .saveAll(argumentCaptor.capture());
        argumentCaptor.getValue().forEach(order -> {
            Assertions.assertEquals(mockWorkerId, order.getWorkerId());
            Assertions.assertEquals(InboxOrderStatus.IN_PROGRESS, order.getStatus());
        });
        Assertions.assertTrue(output.toString().contains("acquired lock successfully"));
        Assertions.assertTrue(output.toString().contains("successfully acquired orders"));
        Assertions.assertTrue(output.toString().contains("successfully released lock"));
        Mockito.verify(distributedLock, Mockito.times(1)).releaseLock(Mockito.any());
    }
}
