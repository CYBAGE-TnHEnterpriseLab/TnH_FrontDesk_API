package com.pms.reservation.integration.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatePlanCalculatedPriceResponseDto {
    private Long ratePlanId;
    private BigDecimal masterBarAmount;
    private BigDecimal finalAmount;
}
