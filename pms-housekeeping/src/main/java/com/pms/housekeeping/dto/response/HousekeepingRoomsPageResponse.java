package com.pms.housekeeping.dto.response;

import java.util.List;

public record HousekeepingRoomsPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<HousekeepingRoomRowResponse> rooms
) {
}


