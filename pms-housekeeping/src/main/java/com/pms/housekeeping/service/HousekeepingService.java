package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HousekeepingService {

    HousekeepingDashboardResponse dashboard(UUID propertyId, LocalDate businessDate);

    HousekeepingRoomsPageResponse rooms(HousekeepingRoomFilterRequest request);

    HousekeepingCalendarResponse calendar(
            UUID propertyId,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> roomTypes
    );

    List<AssignableRoomResponse> assignableRooms(UUID propertyId, LocalDate businessDate, UUID roomTypeId, int limit);

    HousekeepingStatusUpdateResponse updateRoomStatus(String roomNumber, UpdateHousekeepingStatusRequest request);

}


