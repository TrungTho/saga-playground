package com.saga.playground.checkoutservice.basetest;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZookeeperTestConfig {

    @Bean(destroyMethod = "close")
    public TestingServer initTestingServer() {
        try {
            log.info("Starting testing Zookeeper server");
            //! match with the properties source
            return new TestingServer(22181, true);
        } catch (Exception e) {
            log.error("Cannot start Curator testing server");
            throw new RuntimeException(e);
        }
    }

}
