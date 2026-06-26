package com.pms.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationAvailabilityRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotNull(message = "arrivalDate is required")
    private LocalDate arrivalDate;

    @NotNull(message = "departureDate is required")
    private LocalDate departureDate;

    private String roomType;

    @NotNull(message = "adultCount is required")
    @Min(value = 1, message = "adultCount must be >= 1")
    private Integer adultCount = 1;

    @NotNull(message = "childCount is required")
    @Min(value = 0, message = "childCount must be >= 0")
    private Integer childCount = 0;
}
