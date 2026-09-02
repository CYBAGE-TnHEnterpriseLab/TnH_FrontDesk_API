package com.pms.housekeeping.service;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.dto.response.RoomMasterSyncResponse;

public interface RoomMasterSyncService {

    RoomMasterSyncResponse sync(RoomMasterSyncRequest request);

    void deletePropertyData(String propertyId);
}


