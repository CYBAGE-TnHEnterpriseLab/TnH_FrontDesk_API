package com.pms.housekeeping.dto.response;

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
        UUID assignedReservationId,
        boolean sellable,
        LocalDateTime updatedAt
) {
}


