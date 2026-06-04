package com.hotel.pms.frontdesk.guestlisting.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "reservation-service")
public class ReservationServiceProperties {

    @NotBlank(message = "reservation-service.base-url is required")
    private String baseUrl;

    @NotBlank(message = "reservation-service.arrivals-path is required")
    private String arrivalsPath;

    @NotBlank(message = "reservation-service.departures-path is required")
    private String departuresPath;

    @Min(value = 100, message = "reservation-service.connect-timeout-ms must be >= 100")
    private int connectTimeoutMs;

    @Min(value = 100, message = "reservation-service.read-timeout-ms must be >= 100")
    private int readTimeoutMs;
}
