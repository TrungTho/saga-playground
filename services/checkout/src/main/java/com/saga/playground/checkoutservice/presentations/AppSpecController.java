package com.saga.playground.checkoutservice.presentations;

import com.saga.playground.checkoutservice.application.AppSpecService;
import com.saga.playground.checkoutservice.domains.models.HealthCheck;
import com.saga.playground.checkoutservice.utils.http.model.HttpResponseModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppSpecController {
    private final AppSpecService appSpecService;

    /**
     * Health of across-domain services
     *
     * @return Health Check
     */
    @GetMapping("/health")
    public final HttpResponseModel<HealthCheck> healthCheck() {
        return HttpResponseModel.success(appSpecService.healthCheck());
    }

    /**
     * Dummy endpoint for testing 503 handler
     */
    @GetMapping("/unhandled-error")
    public final HttpResponseModel<String> unhandledError() {
        return HttpResponseModel.success(appSpecService.unhandledError());
    }

}
