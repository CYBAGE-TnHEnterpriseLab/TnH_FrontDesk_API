package com.pms.reservation.integration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyRoomInventoryDto {
    @JsonAlias({"roomTypeId", "roomTypeID", "typeId"})
    private Long roomTypeId;

    @JsonAlias({"roomType", "roomTypeName", "typeName"})
    private String roomType;

    private String roomCode;

    @JsonAlias({"occupancy", "occupancyType", "occupancyLabel"})
    private String occupancy;

    @JsonAlias({"availableRooms", "availableCount", "inventoryCount"})
    private Integer availableRooms;

    @JsonAlias({"roomNumber", "roomNo"})
    private String roomNumber;

    @JsonAlias({"floor", "floorNumber"})
    private String floor;
}
