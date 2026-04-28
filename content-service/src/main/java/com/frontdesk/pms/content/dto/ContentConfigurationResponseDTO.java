package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ContentConfigurationResponseDTO {
    private UUID propertyId;
    private SpecialRequestsResponseDTO specialRequests;
    private AmenitiesResponseDTO amenities;
}
