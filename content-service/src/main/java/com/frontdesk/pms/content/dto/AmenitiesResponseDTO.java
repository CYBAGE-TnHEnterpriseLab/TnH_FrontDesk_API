package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenitiesResponseDTO {
    private String airportCode;
    private String distanceJourneyTime;
    private String directions;
    private boolean groundTransportEnabled;
    private boolean shuttleServiceEnabled;
    private boolean swimmingPoolEnabled;
}
