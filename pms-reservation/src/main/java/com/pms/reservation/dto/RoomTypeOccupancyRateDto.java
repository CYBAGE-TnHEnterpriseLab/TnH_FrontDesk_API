package com.pms.reservation.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomTypeOccupancyRateDto {
    Long roomTypeId;
    String roomType;
    Integer guestCount;
    BigDecimal baseRate;
    BigDecimal taxAmount;
    BigDecimal finalAmount;
}

