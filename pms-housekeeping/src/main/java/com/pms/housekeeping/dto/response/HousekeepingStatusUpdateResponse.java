package com.pms.housekeeping.dto.response;

import com.pms.housekeeping.entity.HousekeepingPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HousekeepingStatusUpdateResponse(
        UUID propertyId,
        LocalDate businessDate,
        String roomNumber,
        String cleaningStatus,
        String frontOfficeStatus,
        String reservationStatus,
        String attendantName,
        HousekeepingPriority priority,
        UUID assignedReservationId,
        boolean sellable,
        LocalDateTime updatedAt,
        LocalDateTime lastCleanedAt
) {
}


