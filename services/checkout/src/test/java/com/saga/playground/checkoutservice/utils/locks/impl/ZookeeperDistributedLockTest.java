package com.saga.playground.checkoutservice.utils.locks.impl;

import com.saga.playground.checkoutservice.basetest.ZookeeperTestConfig;
import com.saga.playground.checkoutservice.configs.CuratorConfig;
import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
    ZookeeperTestConfig.class,
    ThreadPoolConfig.class,
    CuratorConfig.class,
    ZookeeperDistributedLock.class
})
@TestPropertySource(properties = {
    "zookeeper.port=22181",
    "zookeeper.host=localhost"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZookeeperDistributedLockTest {

    private final int numberOfTasks = 20;
    private final int longTasKProcessingSeconds = 5;
    private AtomicInteger sharedCounter;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ZookeeperDistributedLock zookeeperDistributedLock;

    @Autowired
    private TestingServer testingServer;

    @MockitoSpyBean
    private ConcurrentHashMap<String, InterProcessMutex> mapLocks;

    @BeforeAll
    void check() {
        Assertions.assertNotNull(testingServer);
    }

    @AfterAll
    void shutdownTestingServer() throws IOException {
        testingServer.stop();
        testingServer.close();
    }

    @BeforeEach
    void setUp() {
        sharedCounter = new AtomicInteger(0);
    }

    void increaseCounter() {
        log.info("{} try to increase counter to {}",
            Thread.currentThread().getName(), sharedCounter.addAndGet(1));
    }

    Runnable normalIncreaseCounterTask() {
        return () -> {
            increaseCounter();
            Awaitility.await().atLeast(1, TimeUnit.SECONDS);
        };
    }

    Runnable lockedIncreaseCounterTask() {
        String lockKey = "/test";
        return () -> {
            if (zookeeperDistributedLock.acquireLock(lockKey)) {
                increaseCounter();

                try {
                    Thread.sleep(longTasKProcessingSeconds * 1000); // simulate a long-running task
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    log.info("{} I will release lock", Thread.currentThread().getName());
                    zookeeperDistributedLock.releaseLock(lockKey);
                }
            } else {
                log.info("{} failed to acquire lock", Thread.currentThread().getName());
            }
        };
    }

    Runnable lockedIncreaseCounterTimeoutTask() {
        int timeout = longTasKProcessingSeconds * 2; // guarantee that can wait until task is finished
        String lockKey = "/test-2";
        return () -> {
            if (zookeeperDistributedLock.acquireLock(lockKey, timeout, TimeUnit.SECONDS)) {
                increaseCounter();

                Awaitility.await().atLeast(longTasKProcessingSeconds, TimeUnit.SECONDS);

                log.info("{} I will release lock", Thread.currentThread().getName());
                zookeeperDistributedLock.releaseLock(lockKey);
            } else {
                log.info("{} failed to acquire lock", Thread.currentThread().getName());
            }
        };
    }

    @Test
    void verifyDataRace() {
        Runnable task = normalIncreaseCounterTask();

        for (int i = 0; i < numberOfTasks; i++) {
            threadPoolExecutor.submit(task);
        }

        Awaitility.await().until(() -> sharedCounter.get() == numberOfTasks);
        Assertions.assertEquals(numberOfTasks, sharedCounter.get(),
            "Counter should be equal with number of tasks");
    }

    @Test
    void testAcquireLock_OK() {
        Runnable task = lockedIncreaseCounterTask();

        for (int i = 0; i < numberOfTasks; i++) {
            threadPoolExecutor.submit(task);
        }

        Awaitility.await()
            .pollDelay(1, TimeUnit.SECONDS) // for threads to start
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
                log.info("completed tasks: {}, active thread: {}",
                    threadPoolExecutor.getCompletedTaskCount(), threadPoolExecutor.getActiveCount());
                return threadPoolExecutor.getActiveCount() == 0;
            });

        Assertions.assertEquals(1, sharedCounter.get(),
            "Counter should be equal with number of tasks");
    }

    @SneakyThrows
    @Test
    void testAcquireLock_Failed() {
        String key = "key";
        int time = 1;
        TimeUnit unit = TimeUnit.SECONDS;

        InterProcessMutex mockMutex = Mockito.mock(InterProcessMutex.class);
        Mockito.when(mapLocks.getOrDefault(key, null))
            .thenReturn(mockMutex);
        Mockito.when(mockMutex.acquire(time, unit))
            .thenThrow(new RuntimeException());

        var res = Assertions.assertDoesNotThrow(
            () -> zookeeperDistributedLock.acquireLock(key, time, unit));
        Assertions.assertFalse(res, "Acquisition should fail in case of exception");
    }

    // in order to verify the order
    @RepeatedTest(10)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testAcquireLockTimeout_OK(CapturedOutput capturedOutput) {
        Runnable task = lockedIncreaseCounterTimeoutTask();

        threadPoolExecutor.submit(task);
        threadPoolExecutor.submit(task);

        Awaitility.await().atMost(3, TimeUnit.SECONDS)
            .until(() -> sharedCounter.get() == 2);

        Assertions.assertEquals(2, sharedCounter.get(),
            "Counter should be increased by both tasks");
        Assertions.assertFalse(capturedOutput.toString().contains("failed to acquire lock"),
            "No lock acquire failures");
    }

    @RepeatedTest(3)
    void testReleaseLock_WrongOwnerShip(CapturedOutput output) {
        String lockKey = "/test-ownership";
        Runnable task = new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                zookeeperDistributedLock.acquireLock(lockKey);
                log.info("Inside thread pool I can acquire lock");
                Thread.sleep(2_000);

                zookeeperDistributedLock.releaseLock(lockKey);
                log.info("Inside thread pool I just released the lock");
            }
        };

        threadPoolExecutor.submit(task);

        Awaitility.await()
            .until(() -> threadPoolExecutor.getActiveCount() == 1);

        zookeeperDistributedLock.releaseLock(lockKey); // same key with task

        Awaitility.await()
            .until(() -> threadPoolExecutor.getActiveCount() == 0);


        // from the task
        Assertions.assertTrue(output.toString().contains("Inside thread pool I can acquire lock"));
        Assertions.assertTrue(output.toString()
            .contains("Inside thread pool I just released the lock"));

        // from this thread
        Assertions.assertTrue(output.toString().contains("Cannot release lock for key"));
        Assertions.assertTrue(output.toString().contains("You do not own the lock: " + lockKey));
    }

    @Test
    void testReleaseLock_NoLock(CapturedOutput output) {
        zookeeperDistributedLock.releaseLock(UUID.randomUUID().toString());

        Assertions.assertTrue(output.toString().contains("NO LOCK"));
    }
}
