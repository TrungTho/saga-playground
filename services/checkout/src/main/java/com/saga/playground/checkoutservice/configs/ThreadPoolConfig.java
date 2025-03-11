package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.ThreadPoolConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
            ThreadPoolConstant.CORE_POOL_SIZE,
            ThreadPoolConstant.MAX_POOL_SIZE,
            ThreadPoolConstant.THREAD_KEEP_ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(ThreadPoolConstant.QUEUE_CAPACITY, true));
    }
}
