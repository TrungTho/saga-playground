package com.saga.playground.checkoutservice.application;


import com.saga.playground.checkoutservice.domains.models.HealthCheck;

public interface AppSpecService {
    HealthCheck healthCheck();

    String unhandledError();
}
