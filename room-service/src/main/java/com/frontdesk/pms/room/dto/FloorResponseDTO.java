package com.frontdesk.pms.room.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FloorResponseDTO {

    private Long id;
    private String name;
    private Long propertyId;
    private Integer floorNumber;
}