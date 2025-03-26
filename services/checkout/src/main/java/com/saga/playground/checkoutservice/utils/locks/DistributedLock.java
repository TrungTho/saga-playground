package com.saga.playground.checkoutservice.utils.locks;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    // acquire & fail immediately
    boolean acquireLock(String key);

    // try to acquire lock & wait for a duration if fails
    boolean acquireLock(String key, int time, TimeUnit timeUnit);

    // release lock
    void releaseLock(String key);
}
