package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AmenitiesResponseDTO {
    private UUID propertyId;
    private String airportCode;
    private String distanceJourneyTime;
    private String directions;
    private boolean groundTransportEnabled;
    private boolean shuttleServiceEnabled;
    private boolean swimmingPoolEnabled;
}
