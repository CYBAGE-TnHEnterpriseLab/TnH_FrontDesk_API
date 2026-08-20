package com.pms.property.domain.content.dto;

public record ContentSummaryResponse(
    String propertyId,
    String propertyDescription,
    String propertyHeroImage,
    long amenitiesCount,
    long nearbyLocationsCount
) {
}

