package com.pms.property.domain.room.service;

import com.pms.property.domain.room.dto.InventoryRoomRequest;
import com.pms.property.domain.room.dto.InventoryRoomResponse;
import com.pms.property.domain.room.dto.RoomOutletTypeResponse;
import com.pms.property.domain.room.dto.RoomSummaryResponse;
import java.util.List;

public interface RoomService {

    RoomSummaryResponse getSummaryByPropertyId(String propertyId);

    List<InventoryRoomResponse> listInventoryRoomsByPropertyId(String propertyId);

    List<RoomOutletTypeResponse> listRoomOutletTypesByPropertyId(String propertyId);

    InventoryRoomResponse getInventoryRoomById(String propertyId, Long roomId);

    InventoryRoomResponse createInventoryRoom(String propertyId, InventoryRoomRequest request);

    InventoryRoomResponse updateInventoryRoom(String propertyId, Long roomId, InventoryRoomRequest request);

    void deleteInventoryRoom(String propertyId, Long roomId);
}


