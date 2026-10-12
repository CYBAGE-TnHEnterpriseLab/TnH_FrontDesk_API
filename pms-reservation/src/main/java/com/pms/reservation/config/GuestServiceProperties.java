package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "pms.services.guest")
public class GuestServiceProperties {

    @NotBlank(message = "pms.services.guest.base-url is required")
    private String baseUrl;

    @Positive(message = "pms.services.guest.connect-timeout-ms must be > 0")
    private int connectTimeoutMs = 3000;

    @Positive(message = "pms.services.guest.read-timeout-ms must be > 0")
    private int readTimeoutMs = 6000;
}
