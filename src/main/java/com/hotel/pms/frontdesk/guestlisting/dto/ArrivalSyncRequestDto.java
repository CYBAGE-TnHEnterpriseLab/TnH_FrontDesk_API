package com.hotel.pms.frontdesk.guestlisting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrivalSyncRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotNull(message = "businessDate is required")
    private LocalDate businessDate;
}
