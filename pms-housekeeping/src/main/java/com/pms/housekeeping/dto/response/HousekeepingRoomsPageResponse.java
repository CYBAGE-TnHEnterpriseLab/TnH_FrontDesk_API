package com.pms.housekeeping.dto.response;

import java.util.List;

public record HousekeepingRoomsPageResponse(

        int page,

        int size,

        long totalElements,

        int totalPages,

        HousekeepingFiltersResponse filterOptions,

        List<HousekeepingRoomRowResponse> rooms

) {
}