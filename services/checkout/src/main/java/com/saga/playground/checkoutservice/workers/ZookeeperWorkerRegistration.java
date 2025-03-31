package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @PostConstruct
    private void init() throws Exception {
        this.register();
    }

    @Override
    public void register() throws Exception {
        log.info("Start registering worker");

        if (distributedLock.acquireLock(
            WorkerConstant.WORKER_REGISTRATION_LOCK,
            WorkerConstant.WORKER_REGISTRATION_WAITING_SECONDS,
            TimeUnit.SECONDS)) {
            log.info("Acquired lock successfully");

            int workerNumber = calculateWorkerNumber();

            this.workerId = curatorClient.create()
                .withProtection()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("%s%d".formatted(WorkerConstant.WORKER_PATH, workerNumber), null);
        } else {
            log.error("CANNOT acquire lock within duration");
            System.exit(1); // somehow the lock having issue
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
        List<String> rawList = curatorClient.getChildren().forPath(WorkerConstant.WORKER_PATH);

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
