package com.saga.playground.checkoutservice.basetest;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@EnableConfigurationProperties
@TestPropertySource(properties = {"zookeeper.port=22181", "zookeeper.host=localhost"})
public abstract class CuratorTestingServerBaseTest {

    private static TestingServer testingServer;

    @BeforeAll
    static void initTestingServer() {
        try {
            testingServer = new TestingServer(22181); //! match with the properties source
        } catch (Exception e) {
            log.error("Cannot start Curator testing server");
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void shutdownTestingServer() {
        CloseableUtils.closeQuietly(testingServer);
    }
    
}
