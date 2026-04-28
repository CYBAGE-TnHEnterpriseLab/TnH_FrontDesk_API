package com.frontdesk.pms.room.mapper;

import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.entity.RoomType;

public class RoomTypeMapper {

    public static RoomTypeResponseDTO toResponse(RoomType entity) {
        return RoomTypeResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .propertyId(entity.getPropertyId())
                .isMaster(entity.getIsMaster())
                .masterRoomTypeId(entity.getMasterRoomTypeId())
                .build();
    }
}