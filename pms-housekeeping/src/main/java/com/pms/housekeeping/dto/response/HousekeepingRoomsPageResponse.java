package com.pms.housekeeping.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record HousekeepingRoomsPageResponse(

        int page,

        int size,

        long totalElements,

        int totalPages,

        HousekeepingFiltersResponse filterOptions,

        List<HousekeepingRoomRowResponse> rooms

) {
}