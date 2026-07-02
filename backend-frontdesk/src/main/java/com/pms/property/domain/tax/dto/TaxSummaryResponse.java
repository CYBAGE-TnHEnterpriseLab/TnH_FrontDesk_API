package com.pms.property.domain.tax.dto;

public record TaxSummaryResponse(
    String propertyId,
    boolean hasTaxProfile,
    long taxRulesCount
) {
}

