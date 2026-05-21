package com.frontdesk.pms.rate_management.dto;

import lombok.Data;

@Data
public class MasterRoomPricingRequestDTO {
    private String occupancyType;
    private Double price;
}