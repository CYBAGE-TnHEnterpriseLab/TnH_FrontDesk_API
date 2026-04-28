package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SpecialRequestsResponseDTO {
    private UUID propertyId;
    private boolean extraPillowEnabled;
    private boolean babyCribEnabled;
    private boolean lateCheckOutEnabled;
    private boolean hypoallergenicBeddingEnabled;
    private boolean airportPickupEnabled;
    private boolean wheelchairAccessEnabled;
}
