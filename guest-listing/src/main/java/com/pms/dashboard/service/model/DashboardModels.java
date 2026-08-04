package com.pms.dashboard.service.model;

import java.math.BigDecimal;
import java.util.UUID;

public final class DashboardModels {

    private DashboardModels() {
    }

    public record HousekeepingDashboardData(
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
        public static HousekeepingDashboardData empty() {
            return new HousekeepingDashboardData(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record HousekeepingRoomData(String roomTypeName, UUID roomTypeId, String cleaningStatus, String frontOfficeStatus, boolean sellable) {
    }

    public record PropertyRoomTypeData(UUID roomTypeId, String roomTypeCode, String roomTypeName) {
    }

    public record InventoryDailyData(int totalInventory, int reservedCount, int blockedCount, int availableCount) {
        public static InventoryDailyData empty() {
            return new InventoryDailyData(0, 0, 0, 0);
        }
    }

    public record ReservationFlowData(long arrivals, long departures) {
        public static ReservationFlowData empty() {
            return new ReservationFlowData(0, 0);
        }
    }

    public record RatePlanData(String name, BigDecimal manualAmount) {
    }
}

