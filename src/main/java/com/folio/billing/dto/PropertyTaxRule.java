package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyTaxRule(
        Long id, String propertyId, String taxName, String type, BigDecimal rate,
        String applicableOn, String inclExcl, LocalDate effectiveDate,
        boolean active, String status, Integer priority
) {
}
