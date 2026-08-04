package com.pms.reservation.integration.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDeductionRequest {

    private String propertyId;
    private String roomType;
    private Integer quantity;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private String confirmationNumber;
}
