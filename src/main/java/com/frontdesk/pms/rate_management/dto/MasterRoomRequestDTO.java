package com.frontdesk.pms.rate_management.dto;

import lombok.Data;
import java.util.List;

@Data
public class MasterRoomRequestDTO {
    private String name;
    private List<MasterRoomPricingRequestDTO> pricingList;
    private List<MasterRoomRoomTypeMappingRequestDTO> roomTypeMappings;
}