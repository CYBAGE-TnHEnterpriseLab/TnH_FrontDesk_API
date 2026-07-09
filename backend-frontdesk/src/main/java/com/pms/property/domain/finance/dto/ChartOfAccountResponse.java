package com.pms.property.domain.finance.dto;

public record ChartOfAccountResponse(
    Long id,
    String propertyId,
    String accountCode,
    String accountName,
    String accountType,
    String ledgerType,
    Boolean active
) {
}

