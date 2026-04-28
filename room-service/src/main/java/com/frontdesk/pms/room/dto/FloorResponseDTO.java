package com.frontdesk.pms.room.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FloorResponseDTO {

    private Long id;
    private String name;
    private UUID propertyId;
    private Integer floorNumber;
}
