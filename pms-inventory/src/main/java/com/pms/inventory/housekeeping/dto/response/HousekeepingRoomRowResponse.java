package com.pms.inventory.housekeeping.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HousekeepingRoomRowResponse(
        String roomNumber,
        UUID roomTypeId,
        String roomTypeName,
        String cleaningStatus,
        String frontOfficeStatus,
        String reservationStatus,
        String guestDisplayName,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String floor,
        String roomClass,
        String zone,
        String attendantName,
        LocalDateTime lastCleanedAt,
        String priority,
        String featuresCsv,
        boolean sellable,
        UUID assignedReservationId
) {
}

