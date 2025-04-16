package com.saga.playground.checkoutservice.configs;

import com.saga.playground.checkoutservice.constants.ThreadPoolConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class EventListenerConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(ThreadPoolConstant.CORE_POOL_SIZE);
        threadPoolTaskExecutor.setMaxPoolSize(ThreadPoolConstant.MAX_POOL_SIZE);
        threadPoolTaskExecutor.setKeepAliveSeconds(ThreadPoolConstant.THREAD_KEEP_ALIVE_TIME_MS);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    static class CustomAsyncExceptionHandler
        implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(
            Throwable throwable, Method method, Object... obj) {

            log.error("Async method {} with params {} throw exception",
                method.getName(), obj, throwable);
        }
    }

}
