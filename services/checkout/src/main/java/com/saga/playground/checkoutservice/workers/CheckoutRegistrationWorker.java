package com.saga.playground.checkoutservice.workers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.annotation.Configuration;

/**
 * Let's say we have multiple workers in our system that try to process the checkout process
 * in parallel, they need to have a way to know the existence of others in order to collaborate
 * Hence this component is using for register an instance with Zookeeper and keep all of them in one place
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CheckoutRegistrationWorker {

    private final CuratorFramework curatorClient;

    private String workerId = "";

    public String getWorkerId() throws Exception {
        if (Strings.isEmpty(workerId)) {
            workerId = registerWorker();
        }

        return workerId;
    }

    public String registerWorker() throws Exception {
        log.info("Try to create new znode");
        return curatorClient.create()
            .withProtection()
            .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
            .forPath("/just-test/worker-", null);
    }

}

