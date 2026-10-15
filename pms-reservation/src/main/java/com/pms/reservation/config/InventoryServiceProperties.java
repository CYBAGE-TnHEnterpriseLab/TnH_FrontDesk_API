package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "inventory-service")
public class InventoryServiceProperties {

    @NotBlank(message = "inventory-service.base-url is required")
    private String baseUrl;

    @NotBlank(message = "inventory-service.availability-path is required")
    private String availabilityPath = "/api/v1/inventory/availability";

    @NotBlank(message = "inventory-service.reservations-path is required")
    private String reservationsPath = "/api/v1/inventory/reservations";

    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 6000;
}
