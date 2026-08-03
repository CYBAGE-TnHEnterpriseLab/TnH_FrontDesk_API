package com.pms.housekeeping.dto.response;

import java.util.List;
import java.util.UUID;

public record HousekeepingFiltersResponse(

        List<RoomTypeOptionResponse> roomTypes,

        List<String> floors,

        List<String> attendants

) {
}