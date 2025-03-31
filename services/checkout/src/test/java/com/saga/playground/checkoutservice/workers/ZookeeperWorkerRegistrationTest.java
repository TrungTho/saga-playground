package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZookeeperWorkerRegistrationTest {

    @Mock
    private CuratorFramework curatorFramework;

    @InjectMocks
    private ZookeeperWorkerRegistration zookeeperWorkerRegistration;

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

}