package com.pms.housekeeping.dto.response;

import java.util.UUID;

public record AssignableRoomResponse(
        String roomNumber,
        UUID roomTypeId,
        String roomTypeName,
        String floor,
        String roomClass,
        String zone,
        String cleaningStatus
) {
}


