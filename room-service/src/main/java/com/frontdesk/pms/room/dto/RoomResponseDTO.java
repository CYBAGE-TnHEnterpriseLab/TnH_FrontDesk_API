package com.frontdesk.pms.room.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponseDTO {

    private Long id;
    private String roomNumber;
    private Long floorId;
    private Long roomTypeId;
}