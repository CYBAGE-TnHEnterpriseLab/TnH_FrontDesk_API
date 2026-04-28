package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomRequestDTO;
import com.frontdesk.pms.room.dto.RoomResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RoomService {

    List<RoomResponseDTO> createRooms(RoomRequestDTO request);

    List<RoomResponseDTO> getAllRooms();

    List<RoomResponseDTO> getRoomsByFloor(Long floorId);

    List<RoomResponseDTO> getRoomsByProperty(UUID propertyId);

    List<RoomResponseDTO> getRoomsByType(Long roomTypeId);

    RoomResponseDTO updateRoom(Long roomId, Long roomTypeId);

    void deleteRoom(Long roomId);
}
