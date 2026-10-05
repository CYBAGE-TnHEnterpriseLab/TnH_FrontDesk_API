package com.pms.loyalty.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoyaltyMembershipResponse {
    UUID membershipId;
    UUID guestId;
    String loyaltyNumber;
    String tierCode;
    String tierName;
    Integer tierLevel;
    String status;
    boolean newlyEnrolled;
}
