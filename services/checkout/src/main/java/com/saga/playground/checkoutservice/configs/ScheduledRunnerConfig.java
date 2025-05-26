package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.WorkerConstant;
import com.saga.playground.checkoutservice.tasks.SingleExecutionQueuedTaskRunner;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutHelper;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutProcessingWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import static com.saga.playground.checkoutservice.constants.ErrorConstant.CODE_UNHANDED_ERROR;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ScheduledRunnerConfig {

    private final ThreadPoolExecutor threadPoolExecutor;

    private final CheckoutProcessingWorker checkoutProcessingWorker;

    private final CheckoutHelper checkoutHelper;

    @Bean(name = "checkoutPullOrderRunner")
    public SingleExecutionQueuedTaskRunner checkoutPullOrderRunner() {
        return new SingleExecutionQueuedTaskRunner(
            WorkerConstant.CHECKOUT_PROCESSING_RUNNER,
            new CheckoutProcessingCoordinator(
                threadPoolExecutor,
                checkoutProcessingWorker,
                checkoutHelper)
        );
    }

    @RequiredArgsConstructor
    static class CheckoutProcessingCoordinator implements Runnable {

        private final ThreadPoolExecutor threadPoolExecutor;

        private final CheckoutProcessingWorker checkoutProcessingWorker;

        private final CheckoutHelper checkoutHelper;

        @Override
        public void run() {
            var runId = UUID.randomUUID().toString();
            log.info("{} Retrieving orders", runId);
            var orders = checkoutProcessingWorker.pullOrders();

            orders.forEach(
                order -> threadPoolExecutor.execute(
                    () -> {
                        try {
                            checkoutProcessingWorker.processCheckout(order.getOrderId());
                            checkoutHelper.postCheckoutProcess(order.getOrderId());
                        } catch (Exception e) {
                            log.error(CODE_UNHANDED_ERROR, e);
                        }
                    }
                )
            );

            log.info("{} Finish submitting {} orders for checking out", runId, orders.size());
        }
    }

}
