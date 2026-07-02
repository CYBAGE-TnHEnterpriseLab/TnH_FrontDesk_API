package com.pms.property.domain.tax.dto;

public record TaxRequest(
    String gstNumber,
    Double taxPercentage
) {
}

