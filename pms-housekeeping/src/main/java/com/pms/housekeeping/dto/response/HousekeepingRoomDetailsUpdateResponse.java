package com.pms.housekeeping.dto.response;

import com.pms.housekeeping.entity.HousekeepingPriority;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HousekeepingRoomDetailsUpdateResponse(
        String propertyId,
        LocalDate businessDate,
        String roomNumber,
        String cleaningStatus,
        String frontOfficeStatus,
        String guestDisplayName,
        String reservationStatus,
        String attendantName,
        List<String> features,
        HousekeepingPriority priority,
        String confirmationId,
        boolean sellable,
        LocalDateTime updatedAt,
        LocalDateTime lastCleanedAt
) {
}


