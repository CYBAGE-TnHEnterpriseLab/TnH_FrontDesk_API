package com.frontdesk.pms.rate_management.dto;
import com.frontdesk.pms.rate_management.enums.DifferentialType;

import lombok.Data;


import java.math.BigDecimal;
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
    private DifferentialType differentialType;
    private BigDecimal differentialValue;
}
