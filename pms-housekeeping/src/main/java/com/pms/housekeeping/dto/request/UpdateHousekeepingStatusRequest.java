package com.pms.housekeeping.dto.request;

import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.StatusChangeSource;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateHousekeepingStatusRequest(
        @NotNull String propertyId,
        @NotNull LocalDate businessDate,
        CleaningStatus cleaningStatus,
        FrontOfficeStatus frontOfficeStatus,
        ReservationStatus reservationStatus,
        String confirmationId,
        String attendantName,
        HousekeepingPriority priority,
        String guestDisplayName,
        LocalDate arrivalDate,
        LocalDate departureDate,
        Boolean sellable,
        String updatedBy,
        @NotNull StatusChangeSource sourceModule,
        LocalDateTime lastCleanedAt
) {
}


