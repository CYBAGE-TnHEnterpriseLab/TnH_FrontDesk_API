package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.dto.HousekeepingRoomStatusResponseDto;

public interface HousekeepingRoomStatusService {

    HousekeepingRoomStatusResponseDto markOccupied(HousekeepingRoomStatusRequestDto request);

    HousekeepingRoomStatusResponseDto markDirty(HousekeepingRoomStatusRequestDto request);

    HousekeepingRoomStatusResponseDto updateManualStatus(HousekeepingRoomStatusRequestDto request, String roomStatus);
}
