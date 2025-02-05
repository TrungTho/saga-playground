package com.saga.playground.checkoutservice.application.impl;


import com.saga.playground.checkoutservice.application.AppSpecService;
import com.saga.playground.checkoutservice.domains.models.HealthCheck;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSpecServiceImpl implements AppSpecService {
    /**
     * Health of across-domain services
     * @return Health Check
     */
    @Override
    public HealthCheck healthCheck() {
        return HealthCheck.builder()
                .isHealthy(true)
                .build();
    }

    /**
     * Getting the version of the running application. It should be passed as an environment variable.
     *
     * @return the version of the running application.
     */
    @Override
    public String getVersion() {
        return System.getenv("CI_COMMIT_SHORT_SHA");
    }

    @Override
    public String liveness() {
        //TODO: add ping for db, kafka and redis here
        return "live";
    }
}
