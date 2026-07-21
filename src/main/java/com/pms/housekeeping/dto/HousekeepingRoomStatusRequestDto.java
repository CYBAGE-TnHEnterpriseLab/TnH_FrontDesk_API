package com.pms.housekeeping.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HousekeepingRoomStatusRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotNull(message = "businessDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate businessDate;

    @NotBlank(message = "confirmationNumber is required")
    private String confirmationNumber;

    private String roomNo;
}
