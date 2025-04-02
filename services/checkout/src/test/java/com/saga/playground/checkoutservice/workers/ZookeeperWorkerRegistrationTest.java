package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.configs.CuratorConfig;
import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.utils.http.error.FatalError;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.stream.Stream;

@Slf4j
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Import({
    ThreadPoolConfig.class,
    CuratorConfig.class,
    ZookeeperDistributedLock.class,
    ZookeeperWorkerRegistration.class,
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // some test use ReflectionTestUtil should be executed last
@TestPropertySource(properties = {"zookeeper.port=22181", "zookeeper.host=localhost"})
class ZookeeperWorkerRegistrationTest {

    @MockitoSpyBean
    private CuratorFramework curatorFramework;

    @MockitoSpyBean
    private ZookeeperDistributedLock zookeeperDistributedLock;

    @Autowired
    private ZookeeperWorkerRegistration zookeeperWorkerRegistration;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    static Stream<Arguments> generateData() {
        return Stream.of(
            Arguments.of("EmptyList", 1, Collections.emptyList()),
            Arguments.of(
                "FullList",
                5,
                List.of(
                    "/workers/_c_901be911-dd40-4485-9dd3-37b6327f8c88-worker-1",
                    "/workers/_c_901be911-dd40-44gawgdd3-37b6327f8c88-worker-2",
                    "/workers/_c_901be911-dabwe-4485-9dd3-37b6327f8c88-worker-3",
                    "/workers/_c_901be911-dd40-4485-9baw3-37b6327f8c88-worker-4"
                )),
            Arguments.of(
                "GapList",
                3,
                List.of(
                    "/workers/_c_901be911-dd40-4485-9dd3-37b6327f8c88-worker-1",
                    "/workers/_c_901be911-dd40-44gawgdd3-37b6327f8c88-worker-2",
                    "/workers/_c_901be911-dabwe-4485-9dd3-37b6327f8c88-worker-5",
                    "/workers/_c_901be911-dd40-4485-9baw3-37b6327f8c88-worker-4"
                )),
            Arguments.of(
                "OverlappedList",
                3,
                List.of(
                    "/workers/_c_901be911-dd40-4485-9dd3-37b6327f8c88-worker-1",
                    "/workers/_c_901be911-dd40-44gawgdd3-37b6327f8c88-worker-2",
                    "/workers/_c_901be911-dabwe-4485-9dd3-37b6327f8c88-worker-2",
                    "/workers/_c_901be911-dd40-4485-9baw3-37b6327f8c88-worker-4"
                ))
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        LogManager.getLogManager().readConfiguration();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generateData")
    void testCalculateWorkerNumber(String testName, int expectedResult, List<String> mockWorkerNames) throws Exception {
        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenReturn(mockWorkerNames);

        int res = zookeeperWorkerRegistration.calculateWorkerNumber();

        Assertions.assertEquals(expectedResult, res, "Result should match with expectedValue");
    }

    @Test
    void testUnhandledError() {
        Mockito.when(zookeeperDistributedLock.acquireLock(
            WorkerConstant.WORKER_REGISTRATION_LOCK,
            WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
            TimeUnit.SECONDS
        )).thenThrow(new RuntimeException());

        var e = Assertions.assertThrows(FatalError.class,
            () -> zookeeperWorkerRegistration.register());

        Assertions.assertEquals(ErrorConstant.CODE_UNHANDED_ERROR, e.getMessage());
    }

    @Order(1)
    @Test
    void testDoubleRegister() throws Exception {
        threadPoolExecutor.submit(() -> {
            try {
                zookeeperWorkerRegistration.register();
                Thread.sleep(10_000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        threadPoolExecutor.submit(() -> {
            try {
                zookeeperWorkerRegistration.register();
                Thread.sleep(10_000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Awaitility.await()
            .until(() -> !zookeeperWorkerRegistration.getWorkerId().isBlank());

        List<String> rawList = curatorFramework.getChildren().forPath("/workers");

        log.info("All list: {}", rawList);
        log.info("worker id: {}", zookeeperWorkerRegistration.getWorkerId());
        Assertions.assertEquals(1, rawList.size(), "Only 1 worker should be register");
        Assertions.assertTrue(rawList.get(0).contains("worker-1"), "Registered worker should be numbered 1");
    }

    @Order(100) // in order for reflection test won't affect other test
    @Test
    void acquireLockFailed(CapturedOutput output) {
        Mockito.when(zookeeperDistributedLock.acquireLock(
                WorkerConstant.WORKER_REGISTRATION_LOCK,
                WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(false);

        ReflectionTestUtils.setField(zookeeperWorkerRegistration, "workerId", "");
        var e = Assertions.assertThrows(FatalError.class,
            () -> zookeeperWorkerRegistration.register());

        Assertions.assertEquals(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED, e.getMessage(),
            "Error message should match");
        Assertions.assertTrue(output.toString().contains("Failed to acquire lock"));
        Assertions.assertTrue(zookeeperWorkerRegistration.getWorkerId().isBlank(),
            "Worker is should be empty");
    }

    @Order(101) // in order for reflection test won't affect other test
    @Test
    void testCuratorCrashingMultipleTime(CapturedOutput output) throws Exception {
        Mockito.when(zookeeperDistributedLock.acquireLock(
                WorkerConstant.WORKER_REGISTRATION_LOCK,
                WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(false);

        ReflectionTestUtils.setField(zookeeperWorkerRegistration, "workerId", "");

        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenThrow(new RuntimeException());

        var e = Assertions.assertThrows(FatalError.class,
            () -> zookeeperWorkerRegistration.register());

        Assertions.assertEquals(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED, e.getMessage(),
            "Error message should match");
        Assertions.assertTrue(output.toString().contains("Failed to acquire lock"));
        Assertions.assertTrue(zookeeperWorkerRegistration.getWorkerId().isBlank(),
            "Worker is should be empty");
    }

    @Test
    void curatorClientCrash() throws Exception {
        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenThrow(new RuntimeException());

        int res = zookeeperWorkerRegistration.calculateWorkerNumber();


        Assertions.assertEquals(-1, res, "Crash should introduce -1 worker");
    }
}
