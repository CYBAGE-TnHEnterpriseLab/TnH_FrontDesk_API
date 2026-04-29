package com.frontdesk.pms.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RoomTypeRequestDTO {

    @NotBlank(message = "Room type name is required")
    private String name;

    // propertyId is set by controller, not required in request body
    private UUID propertyId;

    @NotNull(message = "Master flag is required")
    private Boolean isMaster;

    private Long masterRoomTypeId;
}
