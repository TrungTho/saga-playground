package com.saga.playground.checkoutservice.constants;

public final class ConsumerConstant {
    public static final String ORDER_CREATED_TOPIC = "db.saga_playground.public.orders";
    public static final String ORDER_CREATED_CONSUMER_GROUP_ID = "checkout_service:created_order";

    private ConsumerConstant() {
    }
}
