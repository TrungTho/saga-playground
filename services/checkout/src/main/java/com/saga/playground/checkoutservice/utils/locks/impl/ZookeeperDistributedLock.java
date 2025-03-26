package com.saga.playground.checkoutservice.utils.locks.impl;

import com.saga.playground.checkoutservice.utils.locks.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ZookeeperDistributedLock implements DistributedLock {

    private final CuratorFramework client;

    // https://curator.apache.org/docs/recipes-shared-reentrant-lock
    // as per the note, we shouldn't create new instance for every lock
    // therefore will use a map to cache lock by lockPath
    // Curator will help to differentiate between thread to detect ownership of lock
    // Check unit tests to ensure the behaviors
    private final ConcurrentHashMap<String, InterProcessMutex> mapLocks;


    @Override
    public boolean acquireLock(String key) {
        // https://curator.apache.org/docs/tech-note-02
        return acquireLock(key, 0, TimeUnit.SECONDS);
    }

    @Override
    public boolean acquireLock(String key, int time, TimeUnit timeUnit) {
        InterProcessMutex lock = mapLocks.getOrDefault(key, null);

        if (Objects.isNull(lock)) {
            lock = new InterProcessMutex(client, key);
            mapLocks.putIfAbsent(key, lock);
        } else {
            // check re-entrant
            if (lock.isOwnedByCurrentThread()) {
                return true; // do not acquire again & cause confusion
            }
        }

        try {
            return lock.acquire(time, timeUnit);
        } catch (Exception e) {
            log.error("Cannot acquire lock for key {}", key, e);
            return false;
        }
    }

    @Override
    public void releaseLock(String key) {
        InterProcessMutex lock = mapLocks.getOrDefault(key, null);

        if (!Objects.isNull(lock) && lock.isOwnedByCurrentThread()) {
            try {
                lock.release();
            } catch (Exception e) {
                log.error("Cannot release lock for key {}", key, e);
            }
        }

        // if the current thread doesn't own the lock -> let curator handle it
        // not allow exceeding permission here (release lock owned by another thread)

    }
}
