package com.pms.dashboard.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DashboardConstants {

    public static final long MIN_TIMEOUT_MS = 500L;

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 500;

    public static final String SORT_BY_ROOM_NUMBER = "roomNumber";
    public static final String SORT_ASC = "asc";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_DEGRADED = "DEGRADED";

    public static final String SOURCE_HOUSEKEEPING_SUMMARY = "housekeepingSummary";
    public static final String SOURCE_HOUSEKEEPING_ROOMS_TODAY = "housekeepingRoomsToday";
    public static final String SOURCE_HOUSEKEEPING_ROOMS_TOMORROW = "housekeepingRoomsTomorrow";
    public static final String SOURCE_PROPERTY_ROOM_TYPES = "propertyRoomTypes";
    public static final String SOURCE_INVENTORY = "inventory";
    public static final String SOURCE_RESERVATION_FLOW = "reservationFlow";

    public static final String FRONT_OFFICE_VACANT = "VACANT";
    public static final String FRONT_OFFICE_OCCUPIED = "OCCUPIED";

    public static final String CLEANING_INSPECTED = "INSPECTED";
    public static final String CLEANING_CLEAN = "CLEAN";
    public static final String CLEANING_DIRTY = "DIRTY";
    public static final String CLEANING_PICKUP = "PICKUP";
    public static final String CLEANING_OUT_OF_ORDER = "OUT_OF_ORDER";
    public static final String CLEANING_OUT_OF_SERVICE = "OUT_OF_SERVICE";

    public static final String UNKNOWN_ROOM_TYPE = "Unknown";

    public static final String HOUSEKEEPING_DASHBOARD_SOURCE = "housekeeping.dashboard";
    public static final String HOUSEKEEPING_ROOMS_SOURCE = "housekeeping.rooms";

    public static final String DASHBOARD_BUILD_ERROR =
            "Unable to build frontdesk dashboard response";

    public static final String HOUSEKEEPING_ROOMS_RESPONSE_ERROR =
            "housekeeping.rooms response does not contain rooms array";
}