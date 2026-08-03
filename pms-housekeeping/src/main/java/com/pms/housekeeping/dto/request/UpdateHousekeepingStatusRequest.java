package com.pms.housekeeping.dto.request;

import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingPriority;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.StatusChangeSource;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateHousekeepingStatusRequest(
        @NotNull UUID propertyId,
        @NotNull LocalDate businessDate,
        CleaningStatus cleaningStatus,
        FrontOfficeStatus frontOfficeStatus,
        ReservationStatus reservationStatus,
        UUID assignedReservationId,
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


