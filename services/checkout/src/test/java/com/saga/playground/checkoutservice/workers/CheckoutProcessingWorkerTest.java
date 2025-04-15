package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saga.playground.checkoutservice.constants.GRPCConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.grpc.services.OrderGRPCService;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutHelper;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import com.saga.playground.checkoutservice.workers.workerregistration.CheckoutRegistrationWorker;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.SneakyThrows;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.stream.Stream;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CheckoutProcessingWorkerTest {

    private static final RetryContext RETRY_CONTEXT = Mockito.mock(RetryContext.class);

    private final String mockWorkerId = "worker-1";

    @Mock
    private TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    @Mock
    private CheckoutRegistrationWorker checkoutRegistrationWorker;

    @Mock
    private ZookeeperDistributedLock distributedLock;

    @Mock
    private OrderGRPCService orderGRPCService;

    @Mock
    private CheckoutHelper checkoutHelper;

    @Mock
    private CheckoutRepository checkoutRepository;

    @InjectMocks
    private CheckoutProcessingWorker checkoutProcessingWorker;

    static Stream<Arguments> generateData() {
        return Stream.of(
            Arguments.of(Collections.emptyList()),
            Arguments.of(Instancio
                .ofList(TransactionalInboxOrder.class).size(10).create())
        );
    }

    @BeforeAll
    public static void setUp() {
        RetrySynchronizationManager.register(RETRY_CONTEXT);
    }

    @AfterEach
    void reset() throws Exception {
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    void testProcessCheckout_InvalidOrderId() {
        Assertions.assertDoesNotThrow(() ->
            checkoutProcessingWorker.processCheckout("dummy")
        );
        Mockito.when(RETRY_CONTEXT.getRetryCount()).thenReturn(1);

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .findByOrderId(Mockito.any());
    }

    @Test
    void testProcessCheckout_InvalidAction() {
        Mockito.doThrow(new StatusRuntimeException(Status.UNKNOWN.withDescription(
                GRPCConstant.ORDER_SERVER_INVALID_ACTION)))
            .when(orderGRPCService).switchOrderStatus(1);
        Mockito.when(RETRY_CONTEXT.getRetryCount()).thenReturn(1);

        Assertions.assertDoesNotThrow(() ->
            checkoutProcessingWorker.processCheckout("1")
        );

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .findByOrderId(Mockito.any());
    }

    @Test
    void testProcessCheckout_InvalidActionThrowException() {
        Mockito.doThrow(new StatusRuntimeException(Status.UNKNOWN))
            .when(orderGRPCService).switchOrderStatus(1);
        Mockito.when(RETRY_CONTEXT.getRetryCount()).thenReturn(1);

        Assertions.assertThrows(StatusRuntimeException.class, () ->
            checkoutProcessingWorker.processCheckout("1")
        );

        Mockito.verify(transactionalInboxOrderRepository, Mockito.times(0))
            .findByOrderId(Mockito.any());
    }

    @Test
    void testProcessCheckout_NoOrderInbox(CapturedOutput output) throws JsonProcessingException {
        int orderId = 1;
        Mockito.doNothing().when(orderGRPCService).switchOrderStatus(orderId);
        Mockito.when(transactionalInboxOrderRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(RETRY_CONTEXT.getRetryCount()).thenReturn(1);

        Assertions.assertDoesNotThrow(() ->
            checkoutProcessingWorker.processCheckout("%s".formatted(orderId))
        );

        Assertions.assertTrue(output.toString().contains("INBOX NOT FOUND %d".formatted(orderId)));
        Mockito.verify(checkoutHelper, Mockito.times(0)).buildCheckoutInfo(Mockito.any());
    }

    @Test
    void testProcessCheckout_OK(CapturedOutput output) throws JsonProcessingException {
        int orderId = 1;
        TransactionalInboxOrder mockInbox = Instancio.of(TransactionalInboxOrder.class)
            .set(Select.field(TransactionalInboxOrder::getOrderId), "%d".formatted(orderId))
            .create();
        Checkout mockCheckout = Instancio.of(Checkout.class).create();

        Mockito.doNothing().when(orderGRPCService).switchOrderStatus(orderId);
        Mockito.when(transactionalInboxOrderRepository.findByOrderId(Mockito.any()))
            .thenReturn(Optional.of(mockInbox));
        Mockito.when(RETRY_CONTEXT.getRetryCount()).thenReturn(1);
        Mockito.when(checkoutHelper.buildCheckoutInfo(mockInbox)).thenReturn(mockCheckout);
        Mockito.when(checkoutRepository.save(mockCheckout)).thenReturn(mockCheckout);

        Assertions.assertDoesNotThrow(() ->
            checkoutProcessingWorker.processCheckout("%s".formatted(orderId))
        );

        Assertions.assertFalse(output.toString().contains("Inbox not found %d".formatted(orderId)));
        Assertions.assertTrue(output.toString()
            .contains("Successfully submit checkout request for order %d".formatted(orderId)));
        Mockito.verify(checkoutHelper, Mockito.times(1)).postCheckoutProcess(mockCheckout);
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
    void testPullOrders(CapturedOutput output) throws JsonProcessingException, InterruptedException {
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

        checkoutProcessingWorker.pullOrders();

        Assertions.assertTrue(output.toString().contains(
            "%s found existing orders: %s".formatted(mockWorkerId,
                mockExisitingOrders.stream().map(TransactionalInboxOrder::getOrderId).toList())
        ));
    }

    @Test
    @SneakyThrows
    void testPullOrders_AcquireLockFailed(CapturedOutput output) {
        Mockito.when(checkoutRegistrationWorker.getWorkerId()).thenReturn(mockWorkerId);
        Mockito.when(transactionalInboxOrderRepository
                .findByWorkerIdAndStatus(mockWorkerId, InboxOrderStatus.IN_PROGRESS))
            .thenReturn(Collections.emptyList());
        Mockito.when(distributedLock.acquireLock(WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(false);

        checkoutProcessingWorker.pullOrders();

        Assertions.assertFalse(output.toString().contains("found existing orders"));
        Assertions.assertTrue(output.toString().contains(
            "%s cannot acquire lock to pull new orders".formatted(mockWorkerId)
        ));
        Mockito.verify(distributedLock, Mockito.times(0)).releaseLock(Mockito.any());
    }

    @Test
    void testPullOrders_LockReleaseWhenExceptionThrown(CapturedOutput output) {
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

        Assertions.assertThrows(RuntimeException.class, () -> checkoutProcessingWorker.pullOrders());

        Assertions.assertFalse(output.toString().contains("found existing orders"));
        Assertions.assertTrue(output.toString().contains("Unhandled error"));
        Mockito.verify(distributedLock, Mockito.times(1)).releaseLock(Mockito.any());
    }

    @Test
    void testPullNewOrder_SuccessfullyPullOrders(CapturedOutput output) {
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

        Assertions.assertDoesNotThrow(() -> checkoutProcessingWorker.pullOrders());

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
