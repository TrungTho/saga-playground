package com.saga.playground.checkoutservice.events.checkout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckoutRegisteredListener {

    @Async
    @EventListener
    public void checkoutRegisteredHandler(CheckoutRegisteredEvent event) {
        log.info("I received the message from somewhere {}", event);
    }

}