package com.saga.playground.checkoutservice.workers;

import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("kafkaMessageObjectMapper")
public class CheckoutProcessingWorker {

    // private final TransactionalInboxOrderRepository transactionalInboxOrderRepository;
    //
    // private final CheckoutRegistrationWorker registrationWorker;

    /**
     * method to start checkout process of an order
     *
     * @param orderId id of order
     * @throws ExecutionControl.NotImplementedException
     */
    public void processCheckout(String orderId) throws ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("");
        // grpc call to switch order status

        // fake logic to process order

        // mark done for transactional inbox pattern

        // update checkout status

        // public message for status change (listener)
    }

    /**
     * retrieve from TransactionalInboxOrder if there are any in-progress records belongs to this worker
     *
     * @return orderIds - list id of in-progress order which we can continue to process
     * @throws ExecutionControl.NotImplementedException
     */
    public List<String> retrieveExistingOrder() throws ExecutionControl.NotImplementedException {
        // String workerId = registrationWorker.getWorkerId();
        //
        // log.info("{} start querying existing record", workerId);

        // return transactionalInboxOrderRepository.findByWorkerId(workerId);
        return Collections.emptyList();
    }

    /**
     * check if there is any existing orders we can continue to process
     * otherwise will pull new order from TransactionalInboxOrder table and process them
     *
     * @throws ExecutionControl.NotImplementedException
     */
    public void pullNewOrder() throws ExecutionControl.NotImplementedException {

        var existingOrders = retrieveExistingOrder();
        if (!existingOrders.isEmpty()) {
            for (String orderId : existingOrders) {
                processCheckout(orderId);
            }
        } else {
            throw new ExecutionControl.NotImplementedException("");
            // try to acquire zookeeper log

            // if fail -> wait

            // if successfully acquire lock -> query 100 record

            // set all record to in-progress with worker_id

            // release lock

            // start checking out for all records by calling processCheckout
        }
    }
}
