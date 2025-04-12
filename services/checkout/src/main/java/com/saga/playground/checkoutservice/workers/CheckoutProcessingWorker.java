package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.grpc.services.OrderGRPCService;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("kafkaMessageObjectMapper")
public class CheckoutProcessingWorker {

    private final TransactionalInboxOrderRepository transactionalInboxOrderRepository;

    private final CheckoutRegistrationWorker registrationWorker;

    private final DistributedLock distributedLock;

    private final OrderGRPCService orderGRPCService;

    /**
     * method to start checkout process of an order
     *
     * @param orderId id of order
     */
    @Transactional
    @Retryable // todo: for specific exception
    public void processCheckout(String orderId) {
        log.info("Start checking out order {}", orderId);
        // grpc call to switch order status

        // if switch status fail -> throw exception for retries

        // if switch status OK -> continue

        // query inbox & extract order data

        // persist checkout with init status

        // fake logic to process order

        // mark done for transactional inbox pattern

        // update checkout status

        // public message for status change (listener)
    }

    public void processCheckout(List<TransactionalInboxOrder> orders) {
        for (var order : orders) {
            processCheckout(order.getOrderId());
        }
    }

    /**
     * retrieve from TransactionalInboxOrder if there are any in-progress records belongs to this worker
     *
     * @return orderIds - list id of in-progress order which we can continue to process
     */
    public List<TransactionalInboxOrder> retrieveExistingOrder() {
        String workerId = registrationWorker.getWorkerId();

        log.info("{} start querying existing record", workerId);

        return transactionalInboxOrderRepository.findByWorkerIdAndStatus(workerId, InboxOrderStatus.IN_PROGRESS);
    }

    /**
     * check if there is any existing orders we can continue to process
     * otherwise will pull new order from TransactionalInboxOrder table and process them
     */
    @Transactional
    public void pullNewOrder() {
        var existingOrders = retrieveExistingOrder();
        if (!existingOrders.isEmpty()) {
            log.info("{} found existing orders: {}", registrationWorker.getWorkerId(),
                existingOrders.stream().map(TransactionalInboxOrder::getOrderId).toList());
            processCheckout(existingOrders);
        } else {
            // try to acquire zookeeper log
            List<TransactionalInboxOrder> newOrders = null;
            if (distributedLock.acquireLock(
                WorkerConstant.WORKER_PULL_ORDER_LOCK,
                WorkerConstant.WORKER_PULL_ORDER_LOCK_WAITING_SECONDS,
                TimeUnit.SECONDS)) {
                log.info("{} acquired lock successfully, start querying new orders",
                    registrationWorker.getWorkerId());

                try {
                    // if successfully acquire lock -> query 100 record
                    newOrders = transactionalInboxOrderRepository.findNewOrders();
                    // set all record to in-progress with worker_id
                    newOrders.forEach(order -> {
                        order.setWorkerId(registrationWorker.getWorkerId());
                        order.setStatus(InboxOrderStatus.IN_PROGRESS);
                    });

                    transactionalInboxOrderRepository.saveAll(newOrders);
                    log.info("{} successfully acquired orders {}", registrationWorker.getWorkerId(),
                        newOrders.stream().map(TransactionalInboxOrder::getOrderId).toList());
                } catch (Exception e) {
                    log.error("Unhandled error when {} pulling new orders", registrationWorker.getWorkerId(), e);
                } finally {
                    // release lock
                    distributedLock.releaseLock(WorkerConstant.WORKER_PULL_ORDER_LOCK);
                    log.info("{} successfully released lock", registrationWorker.getWorkerId());
                }
            } else {
                log.info("{} cannot acquire lock to pull new orders", registrationWorker.getWorkerId());
            }

            // start checking out for all records by calling processCheckout
            if (!Objects.isNull(newOrders)) {
                processCheckout(newOrders);
            }
        }
    }
}
