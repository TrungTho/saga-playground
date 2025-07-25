package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.ThreadPoolConstant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@ExtendWith({OutputCaptureExtension.class})
@Slf4j
class ThreadPoolConfigTest {
    private Runnable createNewTask(int finalI, List<Integer> runTasks) {
        return () -> {
            try {
                log.info("Start task {}", finalI);

                // because we don't know which task is "actually" first,
                // so we need this list to keep track of those that will be run
                runTasks.add(finalI);
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                log.info("Stop task {}", finalI);
            }
        };
    }

    @Test
    void testThreadPoolInit(CapturedOutput output) throws InterruptedException {
        var threadPool = new ThreadPoolConfig().threadPoolExecutor();
        List<Integer> runTasks = new ArrayList<>();

        for (int i = 0; i < ThreadPoolConstant.MAX_POOL_SIZE + ThreadPoolConstant.QUEUE_CAPACITY; i++) {
            Runnable task = createNewTask(i, runTasks);

            threadPool.submit(task);
        }

        Assertions.assertEquals(ThreadPoolConstant.MAX_POOL_SIZE, threadPool.getPoolSize());
        Assertions.assertEquals(ThreadPoolConstant.QUEUE_CAPACITY, threadPool.getQueue().size());

        String missingLog = "This can not be logged";
        // if more task is submitted -> assertion will be thrown
        Assertions.assertThrows(RejectedExecutionException.class,
            () -> threadPool.submit(() -> log.info(missingLog)));

        // shutdown all thread in pool
        threadPool.shutdownNow();
        Thread.sleep(100); // for logs to be completely printed

        // assert all thread has stop
        for (var i : runTasks) {
            Assertions.assertTrue(output.toString().contains("Stop task %d".formatted(i)),
                "Task %d should be stopped completely".formatted(i));
        }

        Assertions.assertFalse(output.toString().contains(missingLog));
    }

}
