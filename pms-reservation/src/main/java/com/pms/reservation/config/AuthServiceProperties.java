package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "auth-service")
public class AuthServiceProperties {

    @NotBlank
    private String baseUrl;

    private String verifyPasswordPath = "/api/v1/auth/verify-password";
}