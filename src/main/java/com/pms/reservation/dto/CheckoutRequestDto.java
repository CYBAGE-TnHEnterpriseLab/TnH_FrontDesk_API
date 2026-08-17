package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequestDto {

    @NotBlank(message = "actor is required")
    private String actor;

    @NotNull(message = "businessDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate businessDate;
}
