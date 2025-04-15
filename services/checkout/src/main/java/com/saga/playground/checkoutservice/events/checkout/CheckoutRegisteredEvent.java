package com.saga.playground.checkoutservice.events.checkout;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CheckoutRegisteredEvent extends ApplicationEvent {
    private final String orderId;

    public CheckoutRegisteredEvent(Object source, String orderId) {
        super(source);
        this.orderId = orderId;
    }

}