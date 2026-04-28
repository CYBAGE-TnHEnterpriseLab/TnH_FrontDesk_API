package com.frontdesk.pms.room.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoomTypeResponseDTO {

    private Long id;
    private String name;
    private UUID propertyId;
    private Boolean isMaster;
    private Long masterRoomTypeId;
}
