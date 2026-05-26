package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import java.util.List;

public interface RoomTypeService {

    RoomTypeResponseDTO createRoomType(RoomTypeRequestDTO request);

    List<RoomTypeResponseDTO> getAllRoomTypes();

    RoomTypeResponseDTO getRoomTypeById(Long id);

    RoomTypeResponseDTO updateRoomType(Long id, RoomTypeRequestDTO request);

    void deleteRoomType(Long id);

    /**
     * Fetch all room types for a specific propertyId
     */
    List<RoomTypeResponseDTO> getRoomTypesByPropertyId(java.util.UUID propertyId);
}