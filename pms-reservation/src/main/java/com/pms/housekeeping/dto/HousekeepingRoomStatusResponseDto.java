package com.pms.housekeeping.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HousekeepingRoomStatusResponseDto {
    String propertyId;
    LocalDate businessDate;
    String confirmationNumber;
    String roomNo;
    String roomStatus;
    LocalDateTime updatedAt;
}
