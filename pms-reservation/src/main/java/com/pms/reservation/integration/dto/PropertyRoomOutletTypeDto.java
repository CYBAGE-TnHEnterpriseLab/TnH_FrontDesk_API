package com.pms.reservation.integration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyRoomOutletTypeDto {

    @JsonAlias({"id", "roomTypeId", "typeId"})
    private Long id;

    @JsonAlias({"roomCode", "roomTypeCode", "code", "roomTypeName"})
    private String roomCode;

    @JsonAlias({"roomName", "roomType", "name"})
    private String roomName;
}