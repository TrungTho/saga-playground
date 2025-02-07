package com.saga.playground.checkoutservice.workers;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KafkaCreatedOrderMessage(CDCPayload payload) {

    public record CDCDataAfter(long id,
                               @JsonProperty("user_id") String userId,
                               String status,
                               String amount,
                               String message) {
    }

    public record CDCPayload(CDCDataAfter after) {
    }

}
