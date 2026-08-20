package com.pms.housekeeping.dto.response;

import java.time.LocalDate;
import java.util.List;

public record HousekeepingRoomsPageResponse(

        int page,

        int size,

        long totalElements,

        int totalPages,

//        LocalDate fromDate,
//        LocalDate toDate,

        HousekeepingFiltersResponse filterOptions,

        List<HousekeepingRoomRowResponse> rooms

) {
}