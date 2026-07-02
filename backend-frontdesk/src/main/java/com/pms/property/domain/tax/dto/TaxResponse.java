package com.pms.property.domain.tax.dto;

public record TaxResponse(
    Long id,
    String propertyId,
    String gstNumber,
    Double taxPercentage
) {
}

