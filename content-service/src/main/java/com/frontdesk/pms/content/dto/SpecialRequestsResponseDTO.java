package com.frontdesk.pms.content.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialRequestsResponseDTO {
    private boolean extraPillowEnabled;
    private boolean babyCribEnabled;
    private boolean lateCheckOutEnabled;
    private boolean hypoallergenicBeddingEnabled;
    private boolean airportPickupEnabled;
    private boolean wheelchairAccessEnabled;
}
