package com.frontdesk.pms.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AmenitiesRequestDTO {

    @Pattern(regexp = "(?i)^[A-Z]{3}$", message = "Airport code must be a 3-letter IATA code")
    private String airportCode;

    @Pattern(
            regexp = "(?i)^\\d+(\\.\\d+)?\\s*(km|mi|min|mins|minutes|hr|hrs|hour|hours)$",
            message = "Distance / journey time must contain a number and unit"
    )
    private String distanceJourneyTime;

    @Size(max = 1000, message = "Directions must not exceed 1000 characters")
    private String directions;

    @NotNull
    private Boolean groundTransportEnabled;

    @NotNull
    private Boolean shuttleServiceEnabled;

    @NotNull
    private Boolean swimmingPoolEnabled;
}
