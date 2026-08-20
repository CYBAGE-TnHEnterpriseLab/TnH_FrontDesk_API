package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInCompleteRequestDto {

    @NotBlank(message = "actor is required")
    private String actor;

    @NotNull(message = "businessDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate businessDate;

    @Pattern(regexp = "(?i)ARRIVED|CHECKED_IN", message = "targetStatus must be ARRIVED or CHECKED_IN")
    private String targetStatus;
}
