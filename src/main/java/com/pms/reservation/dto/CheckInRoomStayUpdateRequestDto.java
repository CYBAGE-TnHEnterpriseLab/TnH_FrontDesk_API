package com.pms.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRoomStayUpdateRequestDto {

    @NotBlank(message = "roomType is required")
    private String roomType;

    @NotBlank(message = "roomNo is required")
    private String roomNo;
}
