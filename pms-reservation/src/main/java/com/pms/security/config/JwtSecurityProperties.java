package com.pms.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityProperties {

    @NotBlank(message = "security.jwt.secret is required")
    @Size(min = 32, message = "security.jwt.secret must be at least 32 characters")
    private String secret;
}
