package com.pms.reservation.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "property-wizard-service")
public class PropertyWizardServiceProperties {

    private boolean enabled = false;
    private boolean failOpenOnValidationError = false;
    private boolean failOpenOnWriteError = false;

    // Legacy fields retained only so the existing client can be removed in a later migration.
    // Reservation workflows no longer configure or call these room-inventory operations.
    private String inventoryValidationPath;
    private String inventoryAvailabilityPath;
    private String roomOutletTypesPath;
    private String inventoryDeductionPath;
    private String inventorySyncPath;

    @NotBlank(message = "property-wizard-service.base-url is required")
    private String baseUrl;

    @NotBlank(message = "property-wizard-service.tax-rules-path is required")
    private String taxRulesPath;

}
