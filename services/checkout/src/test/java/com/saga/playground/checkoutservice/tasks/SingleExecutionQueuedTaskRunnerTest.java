package com.saga.playground.checkoutservice.tasks;

import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import com.saga.playground.checkoutservice.domains.entities.Checkout;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutHelper;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SingleExecutionQueuedTaskRunnerTest {

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolConfig().threadPoolExecutor();

    private final String taskName = "Test";

    @Test
    void testTryRun_NoRunningEmptyQueue() {
        // we just need to have a mock for verifying, just an arbitrary class here
        var checkoutHelper = Mockito.mock(CheckoutHelper.class);

        // init new task
        var taskRunner = new SingleExecutionQueuedTaskRunner(taskName,
            () -> checkoutHelper.postCheckoutProcess(Instancio.of(Checkout.class).create().getOrderId()));

        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());

        taskRunner.tryRun();

        Mockito.verify(checkoutHelper, Mockito.times(1))
            .postCheckoutProcess(Mockito.any());
        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());
    }

    @Test
    void testTryRun_RunningEmptyQueue() {
        var mockCheckout = Instancio.of(Checkout.class).create();
        AtomicInteger counter = new AtomicInteger();
        // we just need to have a mock for verifying, just an arbitrary class here
        var checkoutHelper = Mockito.mock(CheckoutHelper.class);
        Mockito.doAnswer(invocation -> {
            Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
            counter.getAndIncrement();
            return null;
        }).when(checkoutHelper).postCheckoutProcess(mockCheckout.getOrderId());

        // init new task
        var taskRunner = new SingleExecutionQueuedTaskRunner(taskName,
            () -> checkoutHelper.postCheckoutProcess(mockCheckout.getOrderId()));

        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());

        // submit the first task
        threadPoolExecutor.submit(taskRunner::tryRun);

        // wait until the task is picked up to run
        Awaitility.await().until(() -> taskRunner.getIsRunning().get());

        // confirm that queue is empty
        Assertions.assertFalse(taskRunner.getIsQueued().get());

        // submit the second task
        threadPoolExecutor.submit(taskRunner::tryRun);

        // isQueued should be true then
        Awaitility.await().until(() -> taskRunner.getIsQueued().get());
        Assertions.assertTrue(taskRunner.getIsQueued().get());

        // wait for both finish
        Awaitility.await().until(() -> counter.get() == 2);

        Mockito.verify(checkoutHelper, Mockito.times(2))
            .postCheckoutProcess(Mockito.any());

        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());
    }

    @Test
    void testTryRun_DiscardRedundancy() {
        int delaySeconds = 1;
        var mockCheckout = Instancio.of(Checkout.class).create();
        AtomicInteger startCounter = new AtomicInteger();
        AtomicInteger finishCounter = new AtomicInteger();
        // we just need to have a mock for verifying, just an arbitrary class here
        var checkoutHelper = Mockito.mock(CheckoutHelper.class);
        Mockito.doAnswer(invocation -> {
            startCounter.getAndIncrement();
            Awaitility.await().pollDelay(delaySeconds, TimeUnit.SECONDS).until(() -> true);
            finishCounter.getAndIncrement();
            return null;
        }).when(checkoutHelper).postCheckoutProcess(mockCheckout.getOrderId());

        // init new task
        var taskRunner = new SingleExecutionQueuedTaskRunner(taskName,
            () -> checkoutHelper.postCheckoutProcess(mockCheckout.getOrderId()));

        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());

        // submit task massively
        int numberOfTasks = 10;
        for (int i = 0; i < numberOfTasks; i++) {
            threadPoolExecutor.submit(taskRunner::tryRun);
        }

        // wait until the task is picked up to run
        // isQueued should be true then
        Awaitility.await().until(() -> taskRunner.getIsRunning().get());

        Awaitility.await().until(() -> taskRunner.getIsQueued().get());
        Assertions.assertTrue(taskRunner.getIsQueued().get());

        // wait for both finish
        Awaitility.await().pollDelay(delaySeconds * 3, TimeUnit.SECONDS)
            .until(() -> true);
        // just to make sure all the task is submitted & executed
        Awaitility.await().until(() -> threadPoolExecutor.getActiveCount() == 0);

        Assertions.assertEquals(2, finishCounter.get());

        // we submit more than 2, but only 2 should be executed
        Mockito.verify(checkoutHelper, Mockito.times(2))
            .postCheckoutProcess(Mockito.any());

        Assertions.assertFalse(taskRunner.getIsRunning().get());
        Assertions.assertFalse(taskRunner.getIsQueued().get());
        Assertions.assertEquals(2, startCounter.get());
    }

}
