package com.pms.reservation.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "reservation.availability")
public class AvailabilityPerformanceProperties {

    /** When false every downstream lookup runs sequentially (previous behaviour). */
    private boolean parallelEnabled = true;

    @Min(value = 1, message = "reservation.availability.parallelism must be >= 1")
    private int parallelism = 12;

    @Min(value = 1, message = "reservation.availability.forecast-days must be >= 1")
    private int forecastDays = 15;

    @Min(value = 1, message = "reservation.availability.timeout-seconds must be >= 1")
    private long timeoutSeconds = 120;
}
