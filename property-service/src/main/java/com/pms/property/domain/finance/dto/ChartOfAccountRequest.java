package com.pms.property.domain.finance.dto;

public record ChartOfAccountRequest(
    String accountCode,
    String accountName,
    String accountType,
    String ledgerType,
    Boolean active
) {
}

