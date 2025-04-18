package com.saga.playground.checkoutservice.workers.checkout;

import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutWorkerTriggerTaskTest {

    @Mock
    private CheckoutProcessingWorker checkoutProcessingWorker;

    @InjectMocks
    private CheckoutWorkerTriggerTask workerTriggerTask;

    private AtomicBoolean isRunning;
    private AtomicBoolean isQueued;

    @BeforeEach
    void setUp() {
        isRunning = (AtomicBoolean) ReflectionTestUtils.getField(CheckoutWorkerTriggerTask.class, "IS_RUNNING");
        isQueued = (AtomicBoolean) ReflectionTestUtils.getField(CheckoutWorkerTriggerTask.class, "IS_QUEUED");
        if (isRunning != null) {
            isRunning.set(false);
        }
        if (isQueued != null) {
            isQueued.set(false);
        }
    }

    @Test
    void triggerPullOrders_noRunningNoQueue() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        workerTriggerTask.triggerPullOrders();

        verify(checkoutProcessingWorker, times(1)).pullOrders();
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
    }


    @Test
    void triggerPullOrders_runningNoQueue() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        when(checkoutProcessingWorker.pullOrders()).thenAnswer(invocation -> {
            Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
            return null;
        });

        var threadPool = new ThreadPoolConfig().threadPoolExecutor();

        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());
        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());

        Awaitility.await().until(() -> threadPool.getActiveCount() == 0);

        // Once for trigger, once for queued
        verify(checkoutProcessingWorker, times(2)).pullOrders();
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
    }

    @Test
    void triggerPullOrders_isQueuedShouldBeSet() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        isRunning.set(true);

        workerTriggerTask.triggerPullOrders();

        verify(checkoutProcessingWorker, times(0)).pullOrders();
        assertTrue(isQueued.get());
    }

    @Test
    void triggerPullOrders_noCap() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        isRunning.set(true);
        isQueued.set(true);

        workerTriggerTask.triggerPullOrders();

        verify(checkoutProcessingWorker, times(0)).pullOrders();
    }

    @Test
    void runQueuedTask_shouldResetIsQueuedAndRunTask() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
        isQueued.set(true);

        workerTriggerTask.runQueuedTask();

        verify(checkoutProcessingWorker, times(1)).pullOrders();
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
    }

    @Test
    void runTask_shouldSetIsRunningExecuteAndResetIsRunning() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        workerTriggerTask.runTask();

        verify(checkoutProcessingWorker, times(1)).pullOrders();
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
    }

    @Test
    void triggerPullOrders_capIsOne() {
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());

        when(checkoutProcessingWorker.pullOrders()).thenAnswer(invocation -> {
            Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
            return null;
        });

        var threadPool = new ThreadPoolConfig().threadPoolExecutor();

        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());
        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());
        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());
        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());
        threadPool.submit(() -> workerTriggerTask.triggerPullOrders());

        Awaitility.await().until(() -> threadPool.getActiveCount() == 0);

        // Once for trigger, once for queued
        verify(checkoutProcessingWorker, times(2)).pullOrders();
        assertFalse(isRunning.get());
        assertFalse(isQueued.get());
    }

}
