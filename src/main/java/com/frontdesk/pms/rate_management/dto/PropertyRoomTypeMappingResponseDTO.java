package com.frontdesk.pms.rate_management.dto;

import lombok.Data;

import java.util.List;

@Data
public class PropertyRoomTypeMappingResponseDTO {
    private Long mappingId;
    private Long roomTypeId;
    private String roomTypeName;
    private boolean mapped;
    private Long masterRoomId;
    private String masterRoomName;
    private List<MasterRoomPricingResponseDTO> inheritedRates;
}
