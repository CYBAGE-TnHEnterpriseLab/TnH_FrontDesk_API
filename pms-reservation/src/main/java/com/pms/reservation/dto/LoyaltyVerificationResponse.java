package com.pms.reservation.dto;

import com.pms.reservation.integration.dto.GuestProfileResponse;
import lombok.Builder;
import lombok.Value;

/**
 * Result of verifying a loyalty number: the membership it is assigned to plus the guest profile
 * that carries it, so the UI can display the guest details for the entered loyalty number.
 */
@Value
@Builder
public class LoyaltyVerificationResponse {
    Boolean valid;
    String loyaltyNumber;
    String tierCode;
    String tierName;
    Integer tierLevel;
    String membershipStatus;
    GuestProfileResponse guest;
}
