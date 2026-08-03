package com.frontdesk.pms.rate_management.dto;

import lombok.Data;

@Data
public class RatePlanPriceResponseDTO {
    private Long ratePlanId;
    private Double masterBarAmount;
    private Double finalAmount;
}
