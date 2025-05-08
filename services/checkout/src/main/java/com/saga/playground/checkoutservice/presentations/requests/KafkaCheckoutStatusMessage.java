package com.saga.playground.checkoutservice.presentations.requests;

import com.saga.playground.checkoutservice.domains.entities.PaymentStatus;

public record KafkaCheckoutStatusMessage(String orderId, PaymentStatus status) {
}
