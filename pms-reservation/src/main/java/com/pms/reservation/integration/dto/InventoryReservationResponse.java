package com.pms.reservation.integration.dto;

import java.time.LocalDate;
import lombok.Value;

@Value
public class InventoryReservationResponse {
    String confirmationNumber;
    String propertyId;
    String bookedRoomTypeId;
    String assignedRoomTypeId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer quantity;
    String status;
    boolean idempotent;
}
