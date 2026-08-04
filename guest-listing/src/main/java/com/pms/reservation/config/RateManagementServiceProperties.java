package com.pms.reservation.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rate-management")
public class RateManagementServiceProperties {

    @jakarta.validation.constraints.NotBlank(message = "rate-management.base-url is required")
    private String baseUrl;

    // Optional: when absent, RM auth interceptor forwards incoming Authorization header.
    private String serviceAuthToken;

    @jakarta.validation.constraints.NotBlank(message = "rate-management.list-rate-plans-path is required")
    private String listRatePlansPath = "/api/rate-plans/property/{propertyId}";

    @jakarta.validation.constraints.NotBlank(message = "rate-management.available-plans-path is required")
    private String availablePlansPath = "/api/rate-plans/property/{propertyId}/available";

    @jakarta.validation.constraints.NotBlank(message = "rate-management.calculated-price-path is required")
    private String calculatedPricePath = "/api/rate-plans/property/{propertyId}/{ratePlanId}/calculated-price";

    @Positive(message = "rate-management.connect-timeout-ms must be > 0")
    private int connectTimeoutMs = 3000;

    @Positive(message = "rate-management.read-timeout-ms must be > 0")
    private int readTimeoutMs = 6000;

    @Min(value = 1, message = "rate-management.retry-max-attempts must be >= 1")
    private int retryMaxAttempts = 2;

    @Min(value = 0, message = "rate-management.retry-backoff-ms must be >= 0")
    private long retryBackoffMs = 100;
}
