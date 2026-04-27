package com.frontdesk.common.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PaymentDTO {
    private UUID propertyId;
    private String gatewayName;
    private String apiKey;
}