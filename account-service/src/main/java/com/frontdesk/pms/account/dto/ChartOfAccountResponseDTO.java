package com.frontdesk.pms.account.dto;

import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChartOfAccountResponseDTO {
    private UUID id;
    private UUID propertyId;
    private String code;
    private String name;
    private AccountType accountType;
    private LedgerType ledgerType;
    private String description;
    private boolean active;
}
