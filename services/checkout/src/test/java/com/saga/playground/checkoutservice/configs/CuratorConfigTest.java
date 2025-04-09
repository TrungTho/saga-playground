package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.basetest.ZookeeperTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(SpringExtension.class)
@Import({
    ZookeeperTestConfig.class,
    CuratorConfig.class
})
@Slf4j
@TestPropertySource(properties = {
    "zookeeper.port=22181",
    "zookeeper.host=localhost"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CuratorConfigTest {

    private final int numberOfWorkers = 100;
    @Autowired
    private CuratorFramework curatorClient;

    @Autowired
    private TestingServer testingServer;

    @BeforeAll
    void check() {
        Assertions.assertNotNull(testingServer);
    }

    @AfterAll
    void shutdownTestingServer() throws IOException {
        testingServer.stop();
        testingServer.close();
    }

    @Test
    void testCurator() {
        Assertions.assertThrows(KeeperException.NoNodeException.class, () -> {
            curatorClient.create().forPath("/my/path", "dummyString".getBytes());
        });

        Assertions.assertDoesNotThrow(() -> {
            String res = curatorClient.create()
                .creatingParentContainersIfNeeded()
                .withProtection()
                .withMode(CreateMode.EPHEMERAL)
                .forPath("%s%d".formatted("/dummypath", 1));

            log.debug("This is the created node: {}", res);
        });
    }

    @Test
    void testDistributedLock() {
        var pool = Executors.newCachedThreadPool();
        AtomicInteger workerCounter = new AtomicInteger(0);
        AtomicInteger sharedValue = new AtomicInteger(0);
        AtomicInteger failedWorker = new AtomicInteger(0);
        String lockKey = "/my-new-lock";

        Runnable task = () -> {
            final int workerId = workerCounter.addAndGet(1);
            log.debug("I am worker {}", workerId);
            InterProcessMutex lock = new InterProcessMutex(curatorClient, lockKey);
            try {
                if (lock.acquire(1, TimeUnit.MILLISECONDS)) {
                    try {
                        log.debug("I am worker {} I can acquire the lock", workerId);
                        sharedValue.addAndGet(1); // expect to be called only once
                        Thread.sleep(10_000);
                    } finally {
                        log.debug("I am worker {} I will release the lock", workerId);
                        lock.release();
                    }
                } else {
                    log.debug("I am worker {} I can't acquire lock within 1 second", workerId);
                    failedWorker.addAndGet(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        for (int i = 0; i < numberOfWorkers; i++) {
            pool.submit(task);
        }
        assertLockBehavior(workerCounter, sharedValue, failedWorker);
    }

    // Testing base on recommendation in the official doc for reusing lock instance
    // https://curator.incubator.apache.org/docs/recipes-shared-reentrant-lock/#general-usage
    @Test
    void testReusingLockInstance() {
        var pool = Executors.newCachedThreadPool();
        AtomicInteger workerCounter = new AtomicInteger(0);
        AtomicInteger sharedValue = new AtomicInteger(0);
        AtomicInteger failedWorker = new AtomicInteger(0);
        String lockKey = "/reuse-lock-instance";

        // the only different
        InterProcessMutex lock = new InterProcessMutex(curatorClient, lockKey);

        Runnable task = () -> {
            final int workerId = workerCounter.addAndGet(1);
            log.debug("I am worker {}", workerId);
            try {
                if (lock.acquire(1, TimeUnit.MILLISECONDS)) {
                    try {
                        log.debug("I am worker {} I can acquire the lock", workerId);
                        sharedValue.addAndGet(1); // expect to be called only once
                        Thread.sleep(10_000);
                    } finally {
                        log.debug("I am worker {} I will release the lock", workerId);
                        lock.release();
                    }
                } else {
                    log.debug("I am worker {} I can't acquire lock within 1 second", workerId);
                    failedWorker.addAndGet(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        for (int i = 0; i < numberOfWorkers; i++) {
            pool.submit(task);
        }

        assertLockBehavior(workerCounter, sharedValue, failedWorker);
    }

    void assertLockBehavior(AtomicInteger workerCounter,
                            AtomicInteger sharedValue, AtomicInteger failedWorker) {
        Awaitility.await()
            .until(() -> failedWorker.get() == numberOfWorkers - 1);

        Assertions.assertEquals(1, sharedValue.get(), "Shared value should be only 1");
        Assertions.assertEquals(numberOfWorkers, workerCounter.get(), "All counter should be initialized");
        Assertions.assertEquals(numberOfWorkers - 1, failedWorker.get(), "Should be n-1 failures");
    }

    @Test
    void testDoubleLock() {
        var pool = Executors.newFixedThreadPool(2); // 2 for just in case
        AtomicInteger sharedCounter = new AtomicInteger(0);
        String lockKey = "/my-new-lock-2";

        Runnable task = () -> {
            InterProcessMutex lock = new InterProcessMutex(curatorClient, lockKey);
            try {
                if (lock.acquire(1, TimeUnit.MILLISECONDS)) {
                    log.debug("{} I can acquire the lock", Thread.currentThread().getName());
                    lock.acquire();
                    log.debug("This is new value {}", sharedCounter.addAndGet(1));
                    log.debug("{} I will try to acquire the lock again",
                        Thread.currentThread().getName());
                } else {
                    log.debug("{} I can't acquire the lock", Thread.currentThread().getName());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        pool.submit(task);

        Awaitility.await()
            .pollInterval(Duration.ofMillis(500))
            .until(() -> sharedCounter.get() == 1);

        pool.submit(task);

        // because the first thread lock twice
        // -> it needs to release lock twice otherwise other candidates will never can acquire lock
        Assertions.assertThrows(ConditionTimeoutException.class,
            () -> Awaitility.await().atLeast(3, TimeUnit.SECONDS)
                .until(() -> sharedCounter.get() > 1));
    }

}
