package com.pms.reservation.integration.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRoomInventoryDto {
    private String roomType;
    private String occupancy;
    private Integer availableRooms;
}
