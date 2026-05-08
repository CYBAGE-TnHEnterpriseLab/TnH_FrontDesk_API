package com.frontdesk.pms.account.mapper;

import com.frontdesk.pms.account.dto.ChartOfAccountResponseDTO;
import com.frontdesk.pms.account.entity.ChartOfAccount;

public final class ChartOfAccountMapper {

    private ChartOfAccountMapper() {
    }

    public static ChartOfAccountResponseDTO toResponse(ChartOfAccount entity) {
        return ChartOfAccountResponseDTO.builder()
                .id(entity.getId())
                .propertyId(entity.getPropertyId())
                .code(entity.getCode())
                .name(entity.getName())
                .accountType(entity.getAccountType())
                .ledgerType(entity.getLedgerType())
                .description(entity.getDescription())
                .active(entity.isActive())
                .build();
    }
}
