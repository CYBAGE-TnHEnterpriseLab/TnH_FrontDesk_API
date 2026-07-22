package com.pms.property.domain.tax.dto;

public record TaxRuleResponse(
    Long id,
    String propertyId,
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

