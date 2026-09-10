package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingRoomDetailsRequest;
import com.pms.housekeeping.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface HousekeepingService {

    HousekeepingDashboardResponse dashboard(String propertyId, LocalDate businessDate);

    HousekeepingRoomsPageResponse rooms(HousekeepingRoomFilterRequest request);

    HousekeepingCalendarResponse calendar(
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> roomTypes
    );

    List<AssignableRoomResponse> assignableRooms(String propertyId, LocalDate businessDate, String roomTypeId, int limit);

    HousekeepingRoomDetailsUpdateResponse updateRoomDetails(String roomNumber, UpdateHousekeepingRoomDetailsRequest request);

}


