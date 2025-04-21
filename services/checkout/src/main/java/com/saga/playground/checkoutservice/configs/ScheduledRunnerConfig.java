package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.tasks.SingleExecutionQueuedTaskRunner;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ScheduledRunnerConfig {

    private final ThreadPoolExecutor threadPoolExecutor;

    private final CheckoutProcessingWorker checkoutProcessingWorker;

    @Bean(name = "checkoutPullOrderRunner")
    public SingleExecutionQueuedTaskRunner checkoutPullOrderRunner() {
        return new SingleExecutionQueuedTaskRunner(
            WorkerConstant.CHECKOUT_PROCESSING_RUNNER,
            new CheckoutCoordinator(threadPoolExecutor, checkoutProcessingWorker)
        );
    }

    @RequiredArgsConstructor
    static class CheckoutCoordinator implements Runnable {

        private final ThreadPoolExecutor threadPoolExecutor;

        private final CheckoutProcessingWorker checkoutProcessingWorker;

        @Override
        public void run() {
            var runId = UUID.randomUUID().toString();
            log.info("{} Retrieving orders", runId);
            var orders = checkoutProcessingWorker.pullOrders();

            orders.forEach(
                order -> threadPoolExecutor.execute(
                    () -> checkoutProcessingWorker.processCheckout(order.getOrderId())
                )
            );

            log.info("{} Finish submitting {} orders for checking out", runId, orders.size());
        }
    }

}
