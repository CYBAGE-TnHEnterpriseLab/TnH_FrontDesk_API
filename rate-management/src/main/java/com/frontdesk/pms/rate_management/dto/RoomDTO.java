package com.frontdesk.pms.rate_management.dto;

import lombok.Data;

@Data
public class RoomDTO {
    private Long id;
    private String name;
    private String type;
    private boolean active;
    // we can add other fields as needed to match the response from room-service
}
