package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "housekeeping-service")
public class HousekeepingServiceProperties {
    @NotBlank private String baseUrl;
    @NotBlank private String roomCalendarPath;
    @NotBlank private String roomStatusPath;
}
