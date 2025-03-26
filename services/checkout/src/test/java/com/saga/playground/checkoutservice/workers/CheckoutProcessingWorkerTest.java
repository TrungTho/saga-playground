package com.saga.playground.checkoutservice.workers;

import jdk.jshell.spi.ExecutionControl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CheckoutProcessingWorkerTest {

    private final CheckoutProcessingWorker checkoutProcessingWorker = new CheckoutProcessingWorker();

    @Test
    void testProcessCheckout() {
        Assertions.assertThrows(ExecutionControl.NotImplementedException.class,
            () -> checkoutProcessingWorker.processCheckout("dummy"));
    }

    @Test
    void testRetrieveExistingOrder() {
        Assertions.assertThrows(ExecutionControl.NotImplementedException.class,
            checkoutProcessingWorker::retrieveExistingOrder);
    }

    @Test
    void testPullNewOrder() {
        Assertions.assertThrows(ExecutionControl.NotImplementedException.class,
            checkoutProcessingWorker::pullNewOrder);
    }
}
