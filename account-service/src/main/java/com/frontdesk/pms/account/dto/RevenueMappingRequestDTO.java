package com.frontdesk.pms.account.dto;

import com.frontdesk.pms.account.enums.ChargeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RevenueMappingRequestDTO {

    @NotNull
    private ChargeType chargeType;

    @NotNull
    private UUID chartOfAccountId;

    private boolean active = true;
}
