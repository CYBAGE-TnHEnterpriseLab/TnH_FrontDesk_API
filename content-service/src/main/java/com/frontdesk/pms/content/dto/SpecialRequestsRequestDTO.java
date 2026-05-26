package com.frontdesk.pms.content.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpecialRequestsRequestDTO {

    @NotNull
    private Boolean extraPillowEnabled;

    @NotNull
    private Boolean babyCribEnabled;

    @NotNull
    private Boolean lateCheckOutEnabled;

    @NotNull
    private Boolean hypoallergenicBeddingEnabled;

    @NotNull
    private Boolean airportPickupEnabled;

    @NotNull
    private Boolean wheelchairAccessEnabled;
}
