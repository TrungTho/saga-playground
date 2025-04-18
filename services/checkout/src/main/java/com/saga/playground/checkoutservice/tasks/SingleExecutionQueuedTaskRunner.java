package com.saga.playground.checkoutservice.tasks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is used to wrap a Runnable.
 * Expected behavior:
 * - runnable will be submitted, if there is no running runnable at current time -> run the runnable
 * - if there is a running runnable which are still running? -> queue the submitted runnable
 * & run it immediately after the current running one
 * - there is a running runnable, and another one in queue -> ignore the new submitted runnable
 * (queue with 1 size)
 */
@Slf4j
@RequiredArgsConstructor
public class SingleExecutionQueuedTaskRunner {
    @Getter
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Getter
    private final AtomicBoolean isQueued = new AtomicBoolean(false);

    @Getter
    private final String taskName;

    // only can be set by constructor
    private final Runnable task;

    // submit a request to run task, no guarantee that this submitted task will be run
    public void tryRun() {
        if (isRunning.compareAndSet(false, true)) {
            runTask();
            if (isQueued.get()) {
                runQueuedTask();
            }
        } else {
            isQueued.compareAndSet(false, true);
        }
        // else: there is a running task, and there is a queued task -> just ignore then
    }

    // run task in queue if there is
    private void runQueuedTask() {
        log.info("Thread {} - task {} starts running the queued task",
            Thread.currentThread().getName(), this.taskName);
        isQueued.set(false);
        runTask();
    }

    // run the task
    private void runTask() {
        log.info("Thread {} - task {} starts running task",
            Thread.currentThread().getName(), this.taskName);
        isRunning.set(true);

        task.run();

        log.info("Thread {}  - task {} finishes running task",
            Thread.currentThread().getName(), this.taskName);
        isRunning.set(false);
    }

}
