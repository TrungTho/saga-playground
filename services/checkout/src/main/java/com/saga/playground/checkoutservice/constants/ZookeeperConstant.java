package com.saga.playground.checkoutservice.constants;

public final class ZookeeperConstant {
    public static final int CLIENT_RETRY_MILLISECONDS = 1_000;
    public static final int CLIENT_MAX_RETRY_TIMES = 3;
    public static final String NAMESPACE = "saga-playground";

    private ZookeeperConstant() {
    }
}
