package com.frontdesk.pms.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RoomTypeRequestDTO {

    @NotBlank(message = "Room type name is required")
    private String name;

    /**
     * propertyId is required for create, ignored for update (immutable).
     * It is set by the controller for create endpoints.
     */
    private UUID propertyId;

    @NotNull(message = "Master flag is required")
    private Boolean isMaster;

    private Long masterRoomTypeId;
}
