package com.saga.playground.checkoutservice.constants;

public final class WorkerConstant {
    public static final String WORKER_REGISTRATION_LOCK = "/worker-registration";
    public static final String WORKER_PATH = "/workers/worker-";
    public static final String WORKER_ID_DELIMITER = "-"; // delimiter before the number id
    public static final int WORKER_REGISTRATION_WAITING_SECONDS = 60;

    private WorkerConstant() {
    }
}
