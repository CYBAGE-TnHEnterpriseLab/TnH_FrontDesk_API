package com.pms.property.domain.tax.dto;

public record TaxSummaryResponse(
    String propertyId,
    boolean hasTaxRules,
    long taxRulesCount
) {
}

