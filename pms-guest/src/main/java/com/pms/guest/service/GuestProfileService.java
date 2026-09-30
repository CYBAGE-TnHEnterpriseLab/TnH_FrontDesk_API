package com.pms.guest.service;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import java.util.List;
import java.util.Optional;

public interface GuestProfileService {

    GuestProfileResponse createGuestProfile(GuestProfileCreateRequest request);

    GuestProfileResponse getGuestProfileById(Long id, String propertyId);

    GuestProfileResponse updateGuestProfile(Long id, String propertyId, GuestProfileUpdateRequest request);

    Optional<GuestProfileResponse> findExistingGuest(GuestLookupRequest request);

    List<GuestProfileResponse> searchGuestProfiles(
            String propertyId,
            String firstName,
            String lastName,
            String phoneNumber,
            String personalEmail,
            String officialEmail,
            String loyaltyMembershipNumber
    );
}
