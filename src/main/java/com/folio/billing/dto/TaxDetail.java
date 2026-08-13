package com.folio.billing.dto;

import java.math.BigDecimal;

public record TaxDetail(String taxName, BigDecimal rate, BigDecimal amount) {
}
