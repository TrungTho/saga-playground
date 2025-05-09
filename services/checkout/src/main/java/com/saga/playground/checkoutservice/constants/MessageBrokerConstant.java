package com.saga.playground.checkoutservice.constants;

public final class MessageBrokerConstant {
    public static final String ORDER_CREATED_TOPIC = "db.saga_playground.public.orders";
    public static final String ORDER_CREATED_CONSUMER_GROUP_ID = "checkout_service:created_order";

    public static final String CHECKOUT_STATUS_TOPIC = "checkout.status.update";

    public static final int ORDER_AMOUNT_SCALE = 2;

    private MessageBrokerConstant() {
    }
}
