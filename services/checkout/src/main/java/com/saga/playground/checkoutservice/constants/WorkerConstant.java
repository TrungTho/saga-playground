package com.saga.playground.checkoutservice.constants;

public final class WorkerConstant {
    public static final String WORKER_REGISTRATION_LOCK = "/worker-registration";
    public static final String WORKER_PATH = "/workers/worker-";
    public static final String WORKER_ID_DELIMITER = "-"; // delimiter before the number id
    public static final int WORKER_REGISTRATION_WAITING_SECONDS = 60;

    public static final String WORKER_PULL_ORDER_LOCK = "/worker-pull-orders";
    public static final int WORKER_PULL_ORDER_LOCK_WAITING_SECONDS = 10 * 60; // ten minutes

    public static final int MAX_RETRY_TIMES = 3;

    public static final int WORKER_CHECKOUT_DELAY_MILLISECONDS = 1_000;

    private WorkerConstant() {
    }
}
