package com.pms.housekeeping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HousekeepingManualStatusUpdateRequestDto extends HousekeepingRoomStatusRequestDto {

    @NotBlank(message = "roomStatus is required")
    @Pattern(regexp = "(?i)OCCUPIED|DIRTY|CLEANED", message = "roomStatus must be OCCUPIED, DIRTY, or CLEANED")
    private String roomStatus;
}
