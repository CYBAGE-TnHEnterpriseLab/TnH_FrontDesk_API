package com.pms.reservation.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomAvailabilityPricingDto {
    Long roomTypeId;
    String roomType;
    String ratePlan;
    String rateCode;
    String occupancy;
    String mealPlan;
    Integer availableRooms;
    BigDecimal baseRate;
    BigDecimal taxAmount;
    BigDecimal finalAmount;
}
