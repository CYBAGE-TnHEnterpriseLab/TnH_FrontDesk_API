package com.pms.reservation.integration.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyTaxRuleResponseDto {
    private String roomType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal taxPercentage;
    private BigDecimal fixedTaxAmount;
    private Boolean active;
}
