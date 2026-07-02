package com.pms.property.domain.finance.dto;

public record FinanceSummaryResponse(
    String propertyId,
    long chartOfAccountsCount,
    long revenueMappingsCount
) {
}

