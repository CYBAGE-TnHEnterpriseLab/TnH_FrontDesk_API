package com.pms.housekeeping.dto.response;

public record HousekeepingDashboardResponse(
        long totalRooms,
        long vacantClean,
        long vacantDirty,
        long occupiedClean,
        long occupiedDirty,
        long outOfOrder,
        long outOfService,
        long inspected,
        long pickup,
        long arrivals,
        long departures
) {
}


