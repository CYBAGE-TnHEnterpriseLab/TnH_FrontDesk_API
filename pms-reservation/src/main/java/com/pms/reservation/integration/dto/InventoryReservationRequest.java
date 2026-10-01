package com.pms.reservation.integration.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InventoryReservationRequest {
    String confirmationNumber;
    String propertyId;
    String bookedRoomTypeId;
    String assignedRoomTypeId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer quantity;
}
