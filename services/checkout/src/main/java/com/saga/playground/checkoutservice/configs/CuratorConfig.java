package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.ZookeeperConstant;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class CuratorConfig {

    @Value("${zookeeper.host}")
    private String zookeeperHost;

    @Value("${zookeeper.port}")
    private String zookeeperPort;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
            ZookeeperConstant.CLIENT_RETRY_MILLISECONDS,
            ZookeeperConstant.CLIENT_MAX_RETRY_TIMES);
        CuratorFramework client = CuratorFrameworkFactory
            .builder()
            .namespace(ZookeeperConstant.NAMESPACE)
            .connectString("%s:%s".formatted(zookeeperHost, zookeeperPort))
            .retryPolicy(retryPolicy)
            .build();
        client.start();
        return client;
    }

    @Bean
    public ConcurrentHashMap<String, InterProcessMutex> mapLocks() {
        return new ConcurrentHashMap<>();  // more configuration can be here
    }

}
