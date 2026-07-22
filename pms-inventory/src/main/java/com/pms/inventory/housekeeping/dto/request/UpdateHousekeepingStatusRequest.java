package com.pms.inventory.housekeeping.dto.request;

import com.pms.inventory.housekeeping.entity.CleaningStatus;
import com.pms.inventory.housekeeping.entity.FrontOfficeStatus;
import com.pms.inventory.housekeeping.entity.HousekeepingPriority;
import com.pms.inventory.housekeeping.entity.ReservationStatus;
import com.pms.inventory.housekeeping.entity.StatusChangeSource;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
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
        String reason
) {
}

