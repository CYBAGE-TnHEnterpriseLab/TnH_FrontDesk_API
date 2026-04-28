package com.frontdesk.pms.room.mapper;

import com.frontdesk.pms.room.dto.FloorResponseDTO;
import com.frontdesk.pms.room.entity.Floor;

public class FloorMapper {

    public static FloorResponseDTO toResponse(Floor floor) {
        return FloorResponseDTO.builder()
                .id(floor.getId())
                .name(floor.getName())
                .propertyId(floor.getPropertyId())
                .floorNumber(floor.getFloorNumber())
                .build();
    }
}