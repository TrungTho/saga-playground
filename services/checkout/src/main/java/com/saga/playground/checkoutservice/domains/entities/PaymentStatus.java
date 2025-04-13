package com.saga.playground.checkoutservice.domains.entities;

public enum PaymentStatus {
    INIT,
    PROCESSING, // request is sent to payment gateway, waiting for response from webhook
    FINALIZED,
    FAILED // it should be more failed cases there, corresponding with appropriate handle
    // but for this playground purpose, we just fail them
}
