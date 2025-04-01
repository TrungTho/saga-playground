package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.utils.http.error.FatalError;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Let's say we have multiple workers in our system that try to process the checkout process
 * in parallel, they need to have a way to know the existence of others in order to collaborate
 * Hence this component is using for register an instance with Zookeeper and keep all of them in one place
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ZookeeperWorkerRegistration implements CheckoutRegistrationWorker {

    private final CuratorFramework curatorClient;

    private final DistributedLock distributedLock;

    @Getter
    private String workerId;

    @Override
    @PostConstruct
    public synchronized void register() {
        log.info("Start registering worker");

        if (!Strings.isBlank(this.workerId)) {
            log.info("Worker was already successfully registered");
            return;
        }

        int retryCount = 1;
        final int retryTimes = 3;

        while (retryCount <= retryTimes) {
            log.info("Try to register the worker, retry time: {} / {}",
                retryCount, retryTimes);

            try {
                if (distributedLock.acquireLock(
                    WorkerConstant.WORKER_REGISTRATION_LOCK,
                    WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
                    TimeUnit.SECONDS)) {
                    log.info("Acquired lock successfully");

                    int workerNumber = calculateWorkerNumber();

                    if (workerNumber != -1) {
                        this.workerId = curatorClient.create()
                            .creatingParentsIfNeeded()
                            .withProtection()
                            .withMode(CreateMode.EPHEMERAL)
                            .forPath("%s%d".formatted(WorkerConstant.WORKER_PATH, workerNumber));
                        break;
                    }
                } else {
                    throw new TimeoutException(ErrorConstant.CODE_TIMEOUT);
                }
            } catch (TimeoutException e) {
                log.info("Failed to acquire lock in the {} / {} attempt",
                    retryCount, retryTimes);
            } catch (Exception e) {
                log.error("Unhandled exception in worker registration", e);
                throw new FatalError(ErrorConstant.CODE_UNHANDED_ERROR);
            }

            retryCount++;
        }

        if (retryCount > retryTimes && this.workerId.isBlank()) {
            throw new FatalError(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED);
        }
    }

    /**
     * In short, we can get a list of N running workers from Zookeeper
     * They should have name from 1 -> N
     * If that is the case, this new worker will be numbered N+1
     * If that is not the case, which means there is a down worker -> this worker will replace that worker
     *
     * @return worker number to register
     */
    public int calculateWorkerNumber() throws Exception {
        List<String> rawList;

        try {
            rawList = curatorClient.getChildren().forPath(WorkerConstant.WORKER_PATH);
        } catch (KeeperException.NoNodeException e) {
            // no worker is there yet, and no parent -> this is the first one so just return 1
            return 1;
        } catch (Exception e) {
            log.error("Cannot get list of existing nodes");
            return -1;
        }

        if (rawList.isEmpty()) {
            return 1;
        }

        List<Integer> workerNumbers = rawList.stream()
            .map(name -> Integer.parseInt(
                name.substring(name.lastIndexOf(WorkerConstant.WORKER_ID_DELIMITER) + 1)))
            .sorted()
            .toList();

        // 1 2 4 5 -> 3
        for (int i = 1; i < workerNumbers.size() + 1; i++) {
            if (!workerNumbers.contains(i)) {
                return i;
            }
        }

        // 1 2 3 4 5 -> 6
        return workerNumbers.size() + 1;
    }
}
