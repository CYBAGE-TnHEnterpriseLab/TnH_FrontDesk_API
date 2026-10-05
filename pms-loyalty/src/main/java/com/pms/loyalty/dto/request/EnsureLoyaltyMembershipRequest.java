package com.pms.loyalty.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EnsureLoyaltyMembershipRequest {
    @NotNull(message = "guestId is required")
    UUID guestId;

    @NotNull(message = "loyaltyProgramId is required")
    UUID loyaltyProgramId;
}
