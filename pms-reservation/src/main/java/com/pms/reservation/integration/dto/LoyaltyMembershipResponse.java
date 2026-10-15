package com.pms.reservation.integration.dto;

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
