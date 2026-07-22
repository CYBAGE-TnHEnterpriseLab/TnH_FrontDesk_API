package com.pms.property.domain.tax.dto;

public record TaxRuleRequest(
    String taxName,
    String type,
    Double rate,
    String applicableOn,
    String inclExcl,
    String effectiveDate,
    Boolean active,
    String status,
    Integer priority
) {
}

