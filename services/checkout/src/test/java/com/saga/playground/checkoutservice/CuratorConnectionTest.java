package com.saga.playground.checkoutservice;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class CuratorConnectionTest {
    @Test
    void testCurator() throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("localhost:22181", retryPolicy);
        client.start();

        Assertions.assertThrows(KeeperException.NoNodeException.class, () -> {
            client.create().forPath("/my/path", "dummyString".getBytes());
        });

        InterProcessMutex lock = new InterProcessMutex(client, "/my-new-lock");
        if (lock.acquire(1, TimeUnit.SECONDS)) {
            try {
                // do some work inside of the critical section here
                System.out.println("I can acquire the lock");

            } finally {
                lock.release();
            }
        } else {
            System.out.println("I can't acquire lock within 1 second");
        }
    }

}
