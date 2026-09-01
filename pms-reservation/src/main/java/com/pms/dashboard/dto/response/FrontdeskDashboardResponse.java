package com.pms.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FrontdeskDashboardResponse(
        UUID propertyId,
        LocalDate businessDate,
        Kpis kpis,
        ComplimentaryHouseUse complimentaryHouseUse,
        RevenueMetrics revenue,
        RoomInventoryMetrics roomInventory,
        HousekeepingRoomStatus housekeeping,
        List<RoomTypeOverview> roomStatusOverview,
        TurndownStatus turndownStatus,
        DailyGuestActivity dailyGuestActivity,
        Map<String, String> sources
) {
    public record Kpis(long availableTonight, long occupiedTonight, double occupancyPercent) {
    }

    public record ComplimentaryHouseUse(
            long arrivals,
            long arrivalsInUse,
            long stayovers,
            long stayoversInUse,
            long departures,
            long departuresInUse
    ) {
    }

    public record RevenueMetrics(BigDecimal roomRevenue, BigDecimal averageRevenue, long individualBookings, long groupBookings) {
    }

    public record RoomInventoryMetrics(long totalRooms, long roomsToSell, long roomsInOrder, long outOfService) {
    }

    public record HousekeepingRoomStatus(Vacant vacant, Occupied occupied) {
    }

    public record Vacant(long inspected, long clean, long dirty, long pickup) {
    }

    public record Occupied(long clean, long pickup, long dirty) {
    }

    public record RoomTypeOverview(String type, long total, long booked, long available) {
    }

    public record TurndownStatus(long required, long notRequired, long completed) {
    }

    public record DailyGuestActivity(Today today, Arrivals arrivals, OtherActivity otherActivity) {
    }

    public record Today(long expected, long checkedIn, long walkIns, long newReservations) {
    }

    public record Arrivals(long expected, long checkedOut, long earlyDepartures) {
    }

    public record OtherActivity(long stayovers, long extendedStays, long dayUseRooms, long sameDayCancels) {
    }
}

