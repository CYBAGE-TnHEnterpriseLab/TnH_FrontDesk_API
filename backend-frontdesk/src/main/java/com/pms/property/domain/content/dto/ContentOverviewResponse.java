package com.pms.property.domain.content.dto;

public record ContentOverviewResponse(
    Long id,
    String propertyId,
    String propertyHeroImage,
    String propertyDescription
) {
}

