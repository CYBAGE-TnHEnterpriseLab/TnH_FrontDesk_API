package com.pms.loyalty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerifyLoyaltyNumberRequest {

    @NotBlank(message = "loyaltyNumber is required")
    @Size(max = 40, message = "loyaltyNumber must not exceed 40 characters")
    String loyaltyNumber;
}
