package com.frontdesk.pms.account.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodResponseDTO {
    private UUID id;
    private String name;
    private UUID propertyId;
    private UUID accountId;
    private boolean allowRefund;
    private boolean active;
}
