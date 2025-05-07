package com.saga.playground.checkoutservice.workers.checkout;

import com.saga.playground.checkoutservice.infrastructure.repositories.CheckoutRepository;
import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("kafkaMessageObjectMapper")
public class CheckoutStatusPublishWorker {

    private final CheckoutRepository checkoutRepository;

    private final DistributedLock distributedLock;

    public void publishCheckoutStatus() {
        // acquire distributed lock with timeout

        // query

        // cannot acquire lock -> log and out
    }

}
