package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentConfigurationResponseDTO {
    private UUID propertyId;
    private String contactName;
    private String email;
    private SpecialRequestsResponseDTO specialRequests;
    private AmenitiesResponseDTO amenities;
}
