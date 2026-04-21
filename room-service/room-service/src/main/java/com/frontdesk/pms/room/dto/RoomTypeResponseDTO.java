package com.frontdesk.pms.room.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomTypeResponseDTO {

    private Long id;
    private String name;
    private Long propertyId;
    private Boolean isMaster;
    private Long masterRoomTypeId;
}