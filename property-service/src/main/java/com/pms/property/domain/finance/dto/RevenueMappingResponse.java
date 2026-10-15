package com.pms.property.domain.finance.dto;

public record RevenueMappingResponse(
        Long id,
        String propertyId,
        String chargeType,
        String mapGlAccount,
        String status,
        String description
) {
}
