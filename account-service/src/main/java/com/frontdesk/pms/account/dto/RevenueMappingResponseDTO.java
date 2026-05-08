package com.frontdesk.pms.account.dto;

import com.frontdesk.pms.account.enums.ChargeType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RevenueMappingResponseDTO {
    private UUID id;
    private UUID propertyId;
    private ChargeType chargeType;
    private UUID chartOfAccountId;
    private boolean active;
}
