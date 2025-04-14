package com.saga.playground.checkoutservice.workers.workerregistration;

public interface CheckoutRegistrationWorker {
    // return the worker id after finished registration
    String getWorkerId();

    // register with the coordination
    void register() throws Exception;
}
