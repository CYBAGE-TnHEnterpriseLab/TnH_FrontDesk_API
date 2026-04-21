package com.frontdesk.pms.room.mapper;

import com.frontdesk.pms.room.dto.RoomResponseDTO;
import com.frontdesk.pms.room.entity.Room;

public class RoomMapper {

    public static RoomResponseDTO toResponse(Room room) {
        return RoomResponseDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floorId(room.getFloorId())
                .roomTypeId(room.getRoomTypeId())
                .build();
    }
}