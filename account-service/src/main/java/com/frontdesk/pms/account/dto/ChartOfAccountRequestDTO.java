package com.frontdesk.pms.account.dto;

import com.frontdesk.common.enums.AccountType;
import com.frontdesk.common.enums.LedgerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChartOfAccountRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private AccountType accountType;

    @NotNull
    private LedgerType ledgerType;

    @Size(max = 500)
    private String description;

    private boolean active = true;
}
