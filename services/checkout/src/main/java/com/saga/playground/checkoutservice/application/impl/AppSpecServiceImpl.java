package com.saga.playground.checkoutservice.application.impl;


import com.saga.playground.checkoutservice.application.AppSpecService;
import com.saga.playground.checkoutservice.domains.models.HealthCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSpecServiceImpl implements AppSpecService {

    /**
     * Health of across-domain services
     *
     * @return Health Check
     */
    @Override
    public HealthCheck healthCheck() {
        return HealthCheck.builder()
            .isHealthy(true)
            .build();
    }

    @Override
    public String unhandledError() {
        throw new RuntimeException("Dummy Exception");
    }

}
