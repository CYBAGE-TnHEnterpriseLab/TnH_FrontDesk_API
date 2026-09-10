package com.pms.housekeeping.dto.response;

import com.pms.housekeeping.entity.HousekeepingPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record HousekeepingRoomRowResponse(
        String roomNumber,
        UUID roomTypeId,
        String roomTypeName,
        String floor,
        String cleaningStatus,
        String frontOfficeStatus,
        String reservationStatus,
        String guestDisplayName,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String attendantName,
        LocalDateTime lastCleanedAt,
        HousekeepingPriority priority,
        boolean sellable,
        String confirmationId,
        List<String> features
) {
}


