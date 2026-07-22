package com.pms.property.integration.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomMasterSyncRequest(
        UUID propertyId,
        LocalDate fromDate,
        LocalDate toDate,
        List<RoomMasterUnit> rooms
) {

    public record RoomMasterUnit(
            UUID roomTypeId,
            String roomTypeName,
            String roomNumber,
            String floor,
            String zone,
            String roomClass,
            String featuresCsv,
            boolean vipCapable,
            boolean active
    ) {
    }
}

