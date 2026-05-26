package com.frontdesk.pms.room.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RoomRequestDTO {

    @NotNull
    private Long floorId;

    @NotNull
    private Long roomTypeId;

    @NotNull
    private UUID propertyId;

    @NotNull
    private Integer numberOfRooms; // how many rooms to create
}
