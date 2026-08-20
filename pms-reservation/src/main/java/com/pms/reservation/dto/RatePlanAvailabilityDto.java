package com.pms.reservation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RatePlanAvailabilityDto {
    String ratePlan;
    String rateCode;
    List<RoomAvailabilityPricingDto> roomTypes;
}