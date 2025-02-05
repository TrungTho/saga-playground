package com.saga.playground.checkoutservice.presentations;

import com.saga.playground.checkoutservice.application.AppSpecService;
import com.saga.playground.checkoutservice.domains.models.HealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppSpecController {
    private final AppSpecService appSpecService;

    /**
     * Health of across-domain services
     * @return Health Check
     */
    @GetMapping("/health")
    public final HealthCheck healthCheck() {
        return appSpecService.healthCheck();
    }

    /**
     * Getting the version of the running application. It should be passed as an environment variable.
     *
     * @return the version of the running application.
     */
    @GetMapping("/version")
    public final String version() {
        return appSpecService.getVersion();
    }

    @GetMapping("/liveness")
    public String liveness() {
        return appSpecService.liveness();
    }
}
