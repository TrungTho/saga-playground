package com.saga.playground.checkoutservice.schedulers;

import com.saga.playground.checkoutservice.tasks.SingleExecutionQueuedTaskRunner;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutStatusPublishWorker;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutWorkerTriggerTask {

    @Qualifier("checkoutPullOrderRunner")
    @NonNull
    private final SingleExecutionQueuedTaskRunner checkoutPullOrderRunner;

    private final CheckoutStatusPublishWorker checkoutStatusPublishWorker;

    @Async(value = "getAsyncExecutor")
    @Scheduled(fixedRateString = "${worker.checkout.interval}")
    public void scheduledPullOrders() {
        checkoutPullOrderRunner.tryRun();
    }

    @Async(value = "getAsyncExecutor")
    @Scheduled(fixedRateString = "${worker.checkout.interval}")
    public void scheduledPublishCheckoutStatus() {
        checkoutStatusPublishWorker.publishCheckoutStatus();
    }
}
