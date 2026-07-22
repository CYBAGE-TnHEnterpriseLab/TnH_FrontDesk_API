package com.pms.housekeeping.dto.response;

public record RoomMasterSyncResponse(
        int syncedRooms,
        int deactivatedRooms
) {
}


