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

    @NotBlank(message = "property-wizard-service.base-url is required")
    private String baseUrl;

    @NotBlank(message = "property-wizard-service.inventory-validation-path is required")
    private String inventoryValidationPath;

    @NotBlank(message = "property-wizard-service.inventory-availability-path is required")
    private String inventoryAvailabilityPath;

    @NotBlank(message = "property-wizard-service.tax-rules-path is required")
    private String taxRulesPath;

    @NotBlank(message = "property-wizard-service.inventory-deduction-path is required")
    private String inventoryDeductionPath;

    @NotBlank(message = "property-wizard-service.inventory-sync-path is required")
    private String inventorySyncPath;
}
