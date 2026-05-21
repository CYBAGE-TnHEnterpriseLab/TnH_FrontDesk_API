package com.frontdesk.pms.rate_management.dto;

import lombok.Data;
import java.util.List;

@Data
public class MasterRoomResponseDTO {
    private Long id;
    private String name;
    private List<MasterRoomPricingResponseDTO> pricingList;
    private List<MasterRoomRoomTypeMappingResponseDTO> roomTypeMappings;
}