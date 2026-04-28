package com.frontdesk.common.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class RoomDTO {
    private UUID propertyId;
    private String roomNumber;
    private Integer floor;
}