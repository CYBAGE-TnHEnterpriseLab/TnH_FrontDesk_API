package com.frontdesk.pms.account.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class PaymentMethodRequestDTO {
    private String name;
    private UUID accountId;
    private boolean allowRefund;
    private boolean active = true;
}
