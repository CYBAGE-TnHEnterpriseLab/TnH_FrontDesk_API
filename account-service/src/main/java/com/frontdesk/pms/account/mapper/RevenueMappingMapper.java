package com.frontdesk.pms.account.mapper;

import com.frontdesk.pms.account.dto.RevenueMappingResponseDTO;
import com.frontdesk.pms.account.entity.RevenueMapping;

public final class RevenueMappingMapper {

    private RevenueMappingMapper() {
    }

    public static RevenueMappingResponseDTO toResponse(RevenueMapping entity) {
        return RevenueMappingResponseDTO.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .chargeType(entity.getChargeType())
                .chartOfAccountId(entity.getChartOfAccountId())
                .active(entity.isActive())
                .build();
    }
}
