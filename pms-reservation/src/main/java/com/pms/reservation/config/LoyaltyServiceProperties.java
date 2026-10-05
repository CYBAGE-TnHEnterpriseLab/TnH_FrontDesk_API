package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "loyalty-service")
public class LoyaltyServiceProperties {

    @NotBlank(message = "loyalty-service.base-url is required")
    private String baseUrl;

    private String defaultProgramId;
}
