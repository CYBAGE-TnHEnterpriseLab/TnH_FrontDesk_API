package com.pms.reservation.integration.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyInventoryValidationResponse {

    private Boolean propertyExists;
    private Boolean roomTypeAvailable;
    private Integer availableRooms;
}
