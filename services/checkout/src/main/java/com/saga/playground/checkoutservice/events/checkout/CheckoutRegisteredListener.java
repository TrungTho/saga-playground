package com.saga.playground.checkoutservice.events.checkout;

import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;
import com.saga.playground.checkoutservice.presentations.responses.IPNResponse;
import com.saga.playground.checkoutservice.webhooks.PaymentGatewayHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckoutRegisteredListener {

    private final PaymentGatewayHandler paymentGatewayHandler;

    @Async
    @EventListener
    public void checkoutRegisteredHandler(CheckoutRegisteredEvent event) {
        log.info("Registered order {}", event.orderId());

        paymentGatewayHandler.simulateWebhookReceived(
            new IPNResponse(event.orderId(), PaymentStatus.FINALIZED));
    }

}
