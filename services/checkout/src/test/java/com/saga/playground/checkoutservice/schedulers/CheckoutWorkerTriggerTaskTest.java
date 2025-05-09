package com.saga.playground.checkoutservice.schedulers;

import com.saga.playground.checkoutservice.configs.ThreadPoolConfig;
import com.saga.playground.checkoutservice.tasks.SingleExecutionQueuedTaskRunner;
import com.saga.playground.checkoutservice.workers.checkout.CheckoutStatusPublishWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

@ExtendWith({SpringExtension.class})
@EnableScheduling
@Import({
    ThreadPoolConfig.class,
    CheckoutWorkerTriggerTask.class,
})
@TestPropertySource(properties = {
    "worker.checkout.interval=5",
    "worker.checkout-status.interval=5",
})
class CheckoutWorkerTriggerTaskTest {

    @MockitoBean
    private SingleExecutionQueuedTaskRunner checkoutRunner;

    @MockitoBean
    private CheckoutStatusPublishWorker checkoutStatusPublishWorker;

    @Autowired
    private CheckoutWorkerTriggerTask checkoutWorkerTriggerTask;

    @Test
    void testScheduledPullOrders() {
        Mockito.doNothing().when(checkoutRunner).tryRun();

        Awaitility.await().pollDelay(25, TimeUnit.MILLISECONDS).until(() -> true);

        Mockito.verify(checkoutRunner, Mockito.atLeast(2)).tryRun();
    }

    @Test
    void testScheduledPublishCheckoutStatus() {
        Mockito.doNothing().when(checkoutStatusPublishWorker).publishCheckoutStatus();

        Awaitility.await().pollDelay(25, TimeUnit.MILLISECONDS).until(() -> true);

        Mockito.verify(checkoutStatusPublishWorker, Mockito.atLeast(2)).
            publishCheckoutStatus();
    }

}
