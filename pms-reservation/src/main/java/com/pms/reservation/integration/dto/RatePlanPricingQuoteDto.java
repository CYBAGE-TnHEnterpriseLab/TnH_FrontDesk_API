package com.pms.reservation.integration.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatePlanPricingQuoteDto {
    private Long roomTypeId;
    private String roomType;
    private String ratePlan;
    private String rateCode;
    private String occupancy;
    private String mealPlan;
    private BigDecimal baseRate;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
}
