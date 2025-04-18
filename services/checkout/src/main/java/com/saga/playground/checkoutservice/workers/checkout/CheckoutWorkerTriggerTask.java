package com.saga.playground.checkoutservice.workers.checkout;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutWorkerTriggerTask {

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    private static final AtomicBoolean IS_QUEUED = new AtomicBoolean(false);

    private final CheckoutProcessingWorker checkoutProcessingWorker;

    @Async(value = "getAsyncExecutor")
    @Scheduled(fixedRate = WorkerConstant.WORKER_CHECKOUT_INTERVAL_MILLISECONDS)
    public void triggerPullOrders() {
        if (IS_RUNNING.compareAndSet(false, true)) {
            runTask();
            if (IS_QUEUED.get()) {
                runQueuedTask();
            }
        } else {
            IS_QUEUED.compareAndSet(false, true);
        }
        // else: there is a running task, and there is a queued task -> just ignore then
    }

    public void runQueuedTask() {
        log.info("Thread {} starts running a queued task", Thread.currentThread().getName());
        IS_QUEUED.set(false);
        runTask();
    }

    public void runTask() {
        log.info("Thread {} starts running task", Thread.currentThread().getName());
        IS_RUNNING.set(true);
        checkoutProcessingWorker.pullOrders();
        log.info("Thread{} finishes running task", Thread.currentThread().getName());
        IS_RUNNING.set(false);
    }

}
