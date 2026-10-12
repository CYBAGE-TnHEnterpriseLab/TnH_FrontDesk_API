package com.pms.reservation.service;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.LoyaltyVerificationResponse;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.LoyaltyServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Verifies a loyalty number against the loyalty service and resolves the guest profile it is assigned to.
 */
@Service
public class LoyaltyVerificationService {

    private static final String INVALID_LOYALTY_NUMBER_MESSAGE =
            "loyalty number invalid and no guest profile found for the given loyalty number";

    private final LoyaltyServiceClient loyaltyServiceClient;
    private final GuestServiceClient guestServiceClient;

    public LoyaltyVerificationService(
            LoyaltyServiceClient loyaltyServiceClient,
            GuestServiceClient guestServiceClient
    ) {
        this.loyaltyServiceClient = loyaltyServiceClient;
        this.guestServiceClient = guestServiceClient;
    }

    public LoyaltyVerificationResponse verifyLoyaltyNumber(String propertyId, String loyaltyNumber) {
        if (!StringUtils.hasText(propertyId)) {
            throw new IllegalArgumentException("propertyId is required");
        }
        if (!StringUtils.hasText(loyaltyNumber)) {
            throw new IllegalArgumentException("loyaltyNumber is required");
        }

        String normalizedLoyaltyNumber = loyaltyNumber.trim();
        String scopedPropertyId = propertyId.trim();

        LoyaltyMembershipResponse membership = findMembership(normalizedLoyaltyNumber);
        GuestProfileResponse guest = findGuestProfile(scopedPropertyId, normalizedLoyaltyNumber);

        return LoyaltyVerificationResponse.builder()
                .valid(Boolean.TRUE)
                .loyaltyNumber(normalizedLoyaltyNumber)
                .tierCode(membership.getTierCode())
                .tierName(membership.getTierName())
                .tierLevel(membership.getTierLevel())
                .membershipStatus(membership.getStatus())
                .guest(guest)
                .build();
    }

    private LoyaltyMembershipResponse findMembership(String loyaltyNumber) {
        try {
            LoyaltyMembershipResponse membership =
                    loyaltyServiceClient.findMembershipByLoyaltyNumber(loyaltyNumber);
            if (membership == null || !StringUtils.hasText(membership.getLoyaltyNumber())) {
                throw invalidLoyaltyNumber(loyaltyNumber);
            }
            return membership;
        } catch (ResponseStatusException ex) {
            throw invalidLoyaltyNumber(loyaltyNumber);
        }
    }

    private GuestProfileResponse findGuestProfile(String propertyId, String loyaltyNumber) {
        GuestLookupRequest lookup = GuestLookupRequest.builder()
                .propertyId(propertyId)
                .loyaltyNumber(loyaltyNumber)
                .build();

        Optional<GuestProfileResponse> profile = guestServiceClient.findExistingGuest(lookup);
        if (profile.isEmpty()) {
            throw invalidLoyaltyNumber(loyaltyNumber);
        }
        return profile.get();
    }

    private ResponseStatusException invalidLoyaltyNumber(String loyaltyNumber) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, INVALID_LOYALTY_NUMBER_MESSAGE);
    }
}
