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

    /**
     * Threads for downstream calls. Day-level tasks block while their IO fan-out runs, so this pool
     * must be comfortably larger than {@link #parallelism}. Defaults to {@code parallelism * 4}.
     */
    @Min(value = 1, message = "reservation.availability.io-parallelism must be >= 1")
    private Integer ioParallelism;

    public int resolveIoParallelism() {
        return ioParallelism == null ? Math.max(1, parallelism) * 4 : ioParallelism;
    }

    @Min(value = 1, message = "reservation.availability.forecast-days must be >= 1")
    private int forecastDays = 15;

    @Min(value = 1, message = "reservation.availability.timeout-seconds must be >= 1")
    private long timeoutSeconds = 120;
}
