package com.saga.playground.checkoutservice.basetest;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.net.BindException;

/**
 * Usually we should use an abstract class for test dependencies initialization
 * But in this case, we have @PostConstruct test in the ZookeeperWorkerRegistrationTest
 * Which requires a Zookeeper server to be there beforehand,
 * Therefore we need to make the Zookeeper testing server as a bean
 */
@TestConfiguration
@Slf4j
public class ZookeeperTestConfig {

    // @Bean(destroyMethod = "close")
    @Bean
    public TestingServer initTestingServer() {
        try {
            log.info("Starting testing Zookeeper server");
            //! match with the properties source
            return new TestingServer(22181, true);
        } catch (BindException e) {
            log.info("Server already started");
            return null;
        } catch (Exception e) {
            log.error("Cannot start Curator testing server");
            throw new RuntimeException(e);
        }
    }

}
