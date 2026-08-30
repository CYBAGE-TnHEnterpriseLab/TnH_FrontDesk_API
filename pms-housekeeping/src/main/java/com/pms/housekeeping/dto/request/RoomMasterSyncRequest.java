package com.pms.housekeeping.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomMasterSyncRequest(
        @NotNull String propertyId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @Valid @NotNull List<RoomMasterUnit> rooms
) {
    public record RoomMasterUnit(
            @NotNull UUID roomTypeId,
            @NotNull String roomTypeName,
            @NotNull String roomNumber,
            String floor,
            String zone,
            String roomClass,
            String featuresCsv,
            boolean vipCapable,
            boolean active
    ) {
    }
}


