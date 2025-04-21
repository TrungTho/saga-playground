package com.saga.playground.checkoutservice.workers.checkout;

import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.constants.GRPCConstant;
import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.domains.entities.InboxOrderStatus;
import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.domains.entities.TransactionalInboxOrder;
import com.saga.playground.checkoutservice.grpc.services.OrderGRPCService;
import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.infrastructure.repositories.TransactionalInboxOrderRepository;
import com.saga.playground.checkoutservice.utils.http.error.CommonHttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import com.saga.playground.checkoutservice.workers.workerregistration.CheckoutRegistrationWorker;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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

    private final CheckoutRepository checkoutRepository;

    private final CheckoutHelper checkoutHelper;

    void updateFailedInbox(String orderId, Exception e) {
        transactionalInboxOrderRepository.findByOrderId(orderId)
            .ifPresent(inboxOrder -> {
                inboxOrder.setStatus(InboxOrderStatus.FAILED);
                inboxOrder.setNote(e.getMessage());
            });
    }

    /**
     * method to start checkout process of an order
     *
     * @param orderId id of order
     */
    @Transactional
    @Retryable(recover = "recoverCheckoutFailed", maxAttempts = WorkerConstant.MAX_RETRY_TIMES)
    public void processCheckout(String orderId) {
        log.info("Start checking out order {}, retry {}",
            orderId,
            Objects.requireNonNull(RetrySynchronizationManager.getContext()).getRetryCount());
        // grpc call to switch order status
        try {
            orderGRPCService.switchOrderStatus(Integer.parseInt(orderId));
        } catch (NumberFormatException e) {
            log.error("INVALID ORDER FORMAT {}", orderId); // no retry
            updateFailedInbox(orderId, e);
            return;
        } catch (StatusRuntimeException e) {
            if (GRPCConstant.ORDER_SERVER_INVALID_ACTION.equals(e.getStatus().getDescription())) {
                log.info("INVALID ACTION {}", orderId); // no retry
                updateFailedInbox(orderId, e);
                return;
            } else {
                throw e;
            }
        } // other exception won't be caught & this method will retry

        // if switch status OK -> continue

        // query inbox & extract order data
        var inbox = transactionalInboxOrderRepository.findByOrderId(orderId);
        if (inbox.isEmpty()) {
            log.info("INBOX NOT FOUND {}", orderId);
            return;
        }

        checkoutHelper.upsertCheckoutInfo(inbox.get())
            .ifPresentOrElse(savedCheckout -> {
                // fake logic to process order
                String checkoutSessionId = checkoutHelper.registerCheckout(savedCheckout);

                // mark done for transactional inbox pattern
                inbox.get().setStatus(InboxOrderStatus.DONE);

                // update checkout status
                // info will be saved at the end of the transaction
                savedCheckout.setCheckoutSessionId(checkoutSessionId);
                savedCheckout.setCheckoutStatus(PaymentStatus.PROCESSING);

                checkoutHelper.postCheckoutProcess(savedCheckout);
                log.info("Successfully submit checkout request for order {}", orderId);
            }, () -> updateFailedInbox(orderId, new HttpException(CommonHttpError.ILLEGAL_ARGS)));
    }

    @Recover
    public void recoverCheckoutFailed(Exception e, String orderId) {
        log.error(ErrorConstant.CODE_RETRY_LIMIT_EXCEEDED, orderId, e);
        updateFailedInbox(orderId, e);
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
    public List<TransactionalInboxOrder> pullOrders() {
        var existingOrders = retrieveExistingOrder();
        if (!existingOrders.isEmpty()) {
            log.info("{} found existing orders: {}", registrationWorker.getWorkerId(),
                existingOrders.stream().map(TransactionalInboxOrder::getOrderId).toList());
            return existingOrders;
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
                    throw e;
                } finally {
                    // release lock
                    distributedLock.releaseLock(WorkerConstant.WORKER_PULL_ORDER_LOCK);
                    log.info("{} successfully released lock", registrationWorker.getWorkerId());
                }
            } else {
                log.info("{} cannot acquire lock to pull new orders", registrationWorker.getWorkerId());
            }

            if (!Objects.isNull(newOrders)) {
                return newOrders;
            }

            return Collections.emptyList();
        }
    }
}
