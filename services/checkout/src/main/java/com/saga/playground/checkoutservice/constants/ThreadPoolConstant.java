package com.saga.playground.checkoutservice.constants;

public final class ThreadPoolConstant {
    public static final Integer CORE_POOL_SIZE = 5;
    public static final int MAX_POOL_SIZE = 10;
    public static final int QUEUE_CAPACITY = 20;
    public static final int THREAD_KEEP_ALIVE_TIME_MS = 5000;

    private ThreadPoolConstant() {
    }
}
