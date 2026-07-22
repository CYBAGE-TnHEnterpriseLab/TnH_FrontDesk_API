package com.pms.inventory.housekeeping.dto.response;

public record RoomMasterSyncResponse(
        int syncedRooms,
        int deactivatedRooms
) {
}

