package com.saga.playground.checkoutservice.domains.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthCheck {
    private Boolean isHealthy;
}
