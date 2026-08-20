package com.frontdesk.pms.rate_management.dto;

import lombok.Data;

@Data
public class MasterRoomPricingResponseDTO {
    private String occupancyType;
    private Double price;
}