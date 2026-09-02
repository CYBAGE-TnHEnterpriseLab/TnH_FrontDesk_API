package com.pms.housekeeping.constant;

public class QueryConstants {

    private QueryConstants() {
    }

    public static final String UPDATE_CLEANING_STATUS_FROM_DATE = """
            UPDATE pms_housekeeping.housekeeping_room_day_status r
               SET cleaning_status = :status,
                   last_cleaned_at = :lastCleanedAt,
                   is_sellable = :sellable,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy
             WHERE r.property_id = :propertyId
               AND r.room_number = :roomNumber
               AND r.business_date >= :fromDate
            """;

    public static final String UPDATE_CLEANING_STATUS_FROM_DATE_AFTER_CHECKOUT = """
            UPDATE pms_housekeeping.housekeeping_room_day_status r
               SET cleaning_status = :status,
                   last_cleaned_at = null,
                   is_sellable = :sellable,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy
             WHERE r.property_id = :propertyId
               AND r.room_number = :roomNumber
               AND r.business_date >= :fromDate
            """;

    public static final String FIND_DISTINCT_ROOM_TYPES = """
            select distinct new com.pms.housekeeping.dto.response.RoomTypeOptionResponse(
                   h.roomTypeId,
                   h.roomTypeName
           )
           from HousekeepingRoomDayStatus h
           where h.propertyId = :propertyId
           and h.businessDate = :businessDate
           order by h.roomTypeName
            """;

    public static final String FIND_DISTINCT_FLOORS = """
            select distinct h.floor
           from HousekeepingRoomDayStatus h
           where h.propertyId=:propertyId
           and h.businessDate=:businessDate
           and h.floor is not null
           and trim(h.floor) <> ''
           order by h.floor
            """;

    public static final String FIND_DISTINCT_ATTENDANTS = """
            select distinct h.attendantName
           from HousekeepingRoomDayStatus h
           where h.propertyId = :propertyId
             and h.businessDate = :businessDate
             and h.attendantName is not null
           order by h.attendantName
            """;

    public static final String FIND_CALENDAR_DATA = """
            SELECT r
           FROM HousekeepingRoomDayStatus r
           WHERE r.propertyId = :propertyId
             AND r.businessDate BETWEEN :fromDate AND :toDate
             AND (
                   :roomTypes IS NULL
                   OR r.roomTypeName IN :roomTypes
             )
           ORDER BY r.roomTypeName, r.roomNumber, r.businessDate
            """;
}
