package com.frontdesk.pms.room.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequestDTO {

    @NotNull
    private Long floorId;

    @NotNull
    private Long roomTypeId;

    @NotNull
    private Long propertyId;

    @NotNull
    private Integer numberOfRooms; // how many rooms to create
}