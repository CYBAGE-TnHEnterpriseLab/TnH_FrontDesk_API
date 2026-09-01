package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HousekeepingService {

    HousekeepingDashboardResponse dashboard(String propertyId, LocalDate businessDate);

    HousekeepingRoomsPageResponse rooms(HousekeepingRoomFilterRequest request);

    HousekeepingCalendarResponse calendar(
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> roomTypes
    );

    List<AssignableRoomResponse> assignableRooms(String propertyId, LocalDate businessDate, UUID roomTypeId, int limit);

    RoomFloorResponse roomFloor(String propertyId, String roomNumber);

    HousekeepingStatusUpdateResponse updateRoomStatus(String roomNumber, UpdateHousekeepingStatusRequest request);

}


