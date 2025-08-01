package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.basetest.ZookeeperTestConfig;
import com.saga.playground.checkoutservice.configs.CuratorConfig;
import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.utils.http.error.FatalError;
import com.saga.playground.checkoutservice.utils.locks.impl.ZookeeperDistributedLock;
import com.saga.playground.checkoutservice.workers.workerregistration.ZookeeperWorkerRegistration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.test.TestingServer;
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
    ZookeeperTestConfig.class,
    CuratorConfig.class,
    ZookeeperDistributedLock.class,
    ZookeeperWorkerRegistration.class,
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // some tests use ReflectionTestUtil should be executed last
@TestPropertySource(properties = {
    "zookeeper.port=22181",
    "zookeeper.host=localhost"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // because we are testing a PostConstruct method (register)
class ZookeeperWorkerRegistrationTest {

    @MockitoSpyBean
    private CuratorFramework curatorFramework;

    @MockitoSpyBean
    private ZookeeperDistributedLock zookeeperDistributedLock;

    @Autowired
    private ZookeeperWorkerRegistration zookeeperWorkerRegistration;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private TestingServer testingServer;

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

    static Stream<Arguments> workerIdExtractionData() {
        return Stream.of(
            Arguments.of("/workers/_c_901be911-dd40-4485-9dd3-37b6327f8c88-worker-1",
                "worker-1"),
            Arguments.of("/workers/_c_901be911-dd40-4485-9dd3-37b6327f8c88-worker-2",
                "worker-2"),
            Arguments.of("/worke11-dd40-4485-9dd3-37b6327f8c88-worker-3",
                "worker-3"),
            Arguments.of("/workers/_c_901be911-dd40-4485-37b6327f8c88-worker-4",
                "worker-4")
        );
    }

    @BeforeAll
    void check() {
        Assertions.assertNotNull(testingServer);
    }

    @AfterAll
    void shutdownTestingServer() throws IOException {
        testingServer.stop();
        testingServer.close();
    }

    @AfterEach
    void tearDown() throws IOException {
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    void testRegister_UnhandledError() {
        Mockito.when(curatorFramework.create()).thenThrow(new RuntimeException());

        var e = Assertions.assertThrows(FatalError.class,
            () -> zookeeperWorkerRegistration.register());

        Assertions.assertEquals(ErrorConstant.CODE_UNHANDED_ERROR, e.getMessage());
        Mockito.verify(zookeeperDistributedLock, Mockito.times(1))
            .releaseLock(WorkerConstant.WORKER_REGISTRATION_LOCK);
    }

    @Order(1)
    @Test
    void testRegister_DoubleRegister() throws Exception {
        threadPoolExecutor.submit(() -> {
            try {
                log.info("test method 1 try to acquire lock");
                zookeeperWorkerRegistration.register();
                Thread.sleep(10_000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        threadPoolExecutor.submit(() -> {
            try {
                log.info("test method 2 try to acquire lock");
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
        Assertions.assertTrue(rawList.get(0).contains("worker-1"),
            "Registered worker should be numbered 1");
    }

    @Order(100) // in order for reflection test won't affect other test
    @Test
    void testRegister_AcquireLockFailed(CapturedOutput output) {
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
        Assertions.assertFalse(output.toString().contains("Acquired lock successfully"));
        Assertions.assertTrue(output.toString().contains("Failed to acquire lock"));
        Assertions.assertTrue(zookeeperWorkerRegistration.getWorkerId().isBlank(),
            "Worker is should be empty");
        Mockito.verify(zookeeperDistributedLock, Mockito.times(0))
            .releaseLock(Mockito.any());
    }

    @Order(101) // in order for reflection test won't affect other test
    @Test
    void testRegister_CuratorCrashingMultipleTime(CapturedOutput output) throws Exception {
        Mockito.when(zookeeperDistributedLock.acquireLock(
                WorkerConstant.WORKER_REGISTRATION_LOCK,
                WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
                TimeUnit.SECONDS))
            .thenReturn(true);
        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenThrow(new RuntimeException());

        ReflectionTestUtils.setField(zookeeperWorkerRegistration, "workerId", "");

        var e = Assertions.assertThrows(FatalError.class,
            () -> zookeeperWorkerRegistration.register());

        Assertions.assertEquals(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED, e.getMessage(),
            "Error message should match");
        Assertions.assertFalse(output.toString().contains("Failed to acquire lock"));
        Assertions.assertTrue(output.toString().contains("Acquired lock successfully"));
        Assertions.assertTrue(zookeeperWorkerRegistration.getWorkerId().isBlank(),
            "Worker is should be empty");
        Assertions.assertFalse(output.toString().contains("Registered worker successfully "));
        Mockito.verify(zookeeperDistributedLock, Mockito.times(WorkerConstant.MAX_RETRY_TIMES))
            .releaseLock(WorkerConstant.WORKER_REGISTRATION_LOCK);
        Mockito.verify(zookeeperDistributedLock, Mockito.times(WorkerConstant.MAX_RETRY_TIMES))
            .acquireLock(WorkerConstant.WORKER_REGISTRATION_LOCK,
                WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
                TimeUnit.SECONDS);
    }

    @Test
    void testCalculateWorkerNumber_CuratorClientCrash() throws Exception {
        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenThrow(new RuntimeException());

        int res = zookeeperWorkerRegistration.calculateWorkerNumber();


        Assertions.assertEquals(-1, res, "Crash should introduce -1 worker");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generateData")
    void testCalculateWorkerNumber_OK(
        String testName, int expectedResult, List<String> mockWorkerNames) throws Exception {
        GetChildrenBuilder mockChildrenBuilder = Mockito.mock(GetChildrenBuilder.class);
        Mockito.when(curatorFramework.getChildren())
            .thenReturn(mockChildrenBuilder);
        Mockito.when(mockChildrenBuilder.forPath(WorkerConstant.WORKER_PATH))
            .thenReturn(mockWorkerNames);

        int res = zookeeperWorkerRegistration.calculateWorkerNumber();

        Assertions.assertEquals(expectedResult, res, "Result should match with expectedValue");
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("workerIdExtractionData")
    void testGetWorkerId(String testcase, String expectedValue) {
        ReflectionTestUtils.setField(zookeeperWorkerRegistration, "workerId", testcase);

        String res = zookeeperWorkerRegistration.getWorkerId();

        Assertions.assertEquals(expectedValue, res);
    }

    @Test
    void testGetRawWorkerId() {
        String expectedValue = "thisisadummyid";
        ReflectionTestUtils.setField(zookeeperWorkerRegistration, "workerId", expectedValue);

        String res = zookeeperWorkerRegistration.getRawWorkerId();

        Assertions.assertEquals(expectedValue, res);
    }

}
