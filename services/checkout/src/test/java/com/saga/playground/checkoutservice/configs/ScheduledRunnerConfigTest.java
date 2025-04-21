package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

@ExtendWith(OutputCaptureExtension.class)
class ScheduledRunnerConfigTest {

    private final ThreadPoolExecutor threadPool = new ThreadPoolConfig().threadPoolExecutor();

    private final CheckoutProcessingWorker checkoutProcessingWorker = Mockito.mock(CheckoutProcessingWorker.class);

    @Test
    void testInitBean() {
        var config = new ScheduledRunnerConfig(threadPool, checkoutProcessingWorker);
        var runner = config.checkoutPullOrderRunner();

        Assertions.assertNotNull(runner);
        Assertions.assertEquals(WorkerConstant.CHECKOUT_PROCESSING_RUNNER, runner.getTaskName());
        Assertions.assertFalse(runner.getIsQueued().get());
        Assertions.assertFalse(runner.getIsRunning().get());
    }

    @Test
    void testRun_EmptyOrder(CapturedOutput output) {
        var spyThreadPool = Mockito.spy(threadPool);
        Mockito.when(checkoutProcessingWorker.pullOrders()).thenReturn(Collections.emptyList());

        var checkoutCoordinator =
            new ScheduledRunnerConfig.CheckoutCoordinator(spyThreadPool, checkoutProcessingWorker);

        checkoutCoordinator.run();

        Assertions.assertTrue(output.toString().contains("Retrieving orders"));
        Assertions.assertTrue(output.toString().contains("Finish submitting 0 orders for checking out"));
        Mockito.verify(checkoutProcessingWorker, Mockito.times(1))
            .pullOrders();
        Mockito.verify(checkoutProcessingWorker, Mockito.times(0))
            .processCheckout(Mockito.any());
        Mockito.verify(spyThreadPool, Mockito.times(0))
            .execute(Mockito.any());
    }

    @Test
    void testRun_NonEmptyOrder(CapturedOutput output) {
        var spyThreadPool = Mockito.spy(threadPool);
        int numberOfRecords = 10;
        var mockOrders = Instancio.ofList(TransactionalInboxOrder.class)
            .size(numberOfRecords)
            .create();

        Mockito.when(checkoutProcessingWorker.pullOrders())
            .thenReturn(mockOrders);

        var checkoutCoordinator =
            new ScheduledRunnerConfig.CheckoutCoordinator(spyThreadPool, checkoutProcessingWorker);

        checkoutCoordinator.run();

        Assertions.assertTrue(output.toString().contains("Retrieving orders"));
        Assertions.assertTrue(output.toString().contains("Finish submitting %d orders for checking out"
            .formatted(mockOrders.size())));
        Mockito.verify(checkoutProcessingWorker, Mockito.times(1))
            .pullOrders();
        Mockito.verify(spyThreadPool, Mockito.times(numberOfRecords))
            .execute(Mockito.any());
        mockOrders.forEach(
            order -> Mockito.verify(checkoutProcessingWorker, Mockito.times(1))
                .processCheckout(order.getOrderId()));
    }

}
