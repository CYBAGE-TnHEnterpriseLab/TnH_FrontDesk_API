package com.frontdesk.pms.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FloorRequestDTO {

    @NotBlank(message = "Floor name is required")
    private String name;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Floor number is required")
    private Integer floorNumber;
}
