package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rate-management-service")
public class RateManagementServiceProperties {

    @NotBlank(message = "rate-management-service.base-url is required")
    private String baseUrl;

    @NotBlank(message = "rate-management-service.availability-pricing-path is required")
    private String availabilityPricingPath;
}
