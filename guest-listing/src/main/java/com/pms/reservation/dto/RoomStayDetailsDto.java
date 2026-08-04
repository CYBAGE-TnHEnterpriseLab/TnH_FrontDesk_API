package com.pms.reservation.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomStayDetailsDto {
    LocalDate arrivalDate;
    LocalDate departureDate;
    String roomType;
    String roomNo;
}
