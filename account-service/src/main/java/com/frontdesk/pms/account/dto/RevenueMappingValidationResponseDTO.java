package com.frontdesk.pms.account.dto;

import com.frontdesk.pms.account.enums.ChargeType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RevenueMappingValidationResponseDTO {
    private UUID propertyId;
    private boolean postingAllowed;
    private List<ChargeType> missingChargeTypes;
}
