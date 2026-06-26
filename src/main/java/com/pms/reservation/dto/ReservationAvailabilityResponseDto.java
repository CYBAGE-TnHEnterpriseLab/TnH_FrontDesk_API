package com.pms.reservation.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationAvailabilityResponseDto {
    String propertyId;
    LocalDate arrivalDate;
    LocalDate departureDate;
    String selectedRoomType;
    List<RoomAvailabilityPricingDto> availability;
}
