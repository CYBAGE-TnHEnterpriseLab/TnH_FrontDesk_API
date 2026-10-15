package com.frontdesk.pms.rate_management.dto;

import com.frontdesk.pms.rate_management.enums.DifferentialType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MasterRoomRoomTypeMappingResponseDTO {

    private Long id;

    private Long roomTypeId;

    private DifferentialType differentialType;

    private BigDecimal differentialValue;
}