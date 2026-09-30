package com.pms.reservation.service;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileCreateRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.GuestProfileUpdateRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class ReservationGuestResolver {

    private static final String TEMPORARY_LOYALTY_NUMBER = "TEMP-GUEST";
    private static final String DEFAULT_LOYALTY_TIER = "STANDARD";

    private final GuestServiceClient guestServiceClient;

    public ReservationGuestResolver(GuestServiceClient guestServiceClient) {
        this.guestServiceClient = guestServiceClient;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ResolvedReservationGuest> resolveGuests(
            String propertyId,
            List<ReservationGuestRequestDto> guests
    ) {
        validateGuestList(propertyId, guests);

        List<Optional<GuestProfileResponse>> resolvedOrPending = new ArrayList<>(guests.size());
        Set<Long> resolvedGuestIds = new HashSet<>();

        for (ReservationGuestRequestDto guest : guests) {
            if (guest.getGuestProfileId() != null) {
                GuestProfileResponse profile = guestServiceClient.getGuestById(guest.getGuestProfileId())
                        .orElseThrow(() -> new BadRequestException(
                                "Guest profile not found: " + guest.getGuestProfileId()));
                verifyProperty(profile, propertyId);
                profile = enrollExistingGuestIfRequested(guest, profile);
                if (!resolvedGuestIds.add(profile.getId())) {
                    throw new BadRequestException("The same guest profile cannot be assigned more than once");
                }
                resolvedOrPending.add(Optional.of(profile));
                continue;
            }

            Optional<GuestProfileResponse> match =
                    guestServiceClient.findExistingGuest(toLookupRequest(propertyId, guest));
            if (match.isPresent()) {
                verifyProperty(match.get(), propertyId);
                GuestProfileResponse resolvedMatch = enrollExistingGuestIfRequested(guest, match.get());
                match = Optional.of(resolvedMatch);
                if (!resolvedGuestIds.add(resolvedMatch.getId())) {
                    throw new BadRequestException("The same guest profile cannot be assigned more than once");
                }
            }
            resolvedOrPending.add(match);
        }

        List<ResolvedReservationGuest> resolved = new ArrayList<>(guests.size());
        for (int index = 0; index < guests.size(); index++) {
            ReservationGuestRequestDto request = guests.get(index);
            GuestProfileResponse profile = resolvedOrPending.get(index).orElseGet(() -> {
                GuestProfileResponse created = guestServiceClient.createGuest(toCreateRequest(propertyId, request));
                verifyProperty(created, propertyId);
                return created;
            });
            if (!resolvedGuestIds.add(profile.getId())
                    && !resolvedOrPending.get(index).isPresent()) {
                throw new BadRequestException("The same guest profile cannot be assigned more than once");
            }
            resolved.add(new ResolvedReservationGuest(profile.getId(), request.getIsPrimary()));
        }
        return List.copyOf(resolved);
    }

    private void validateGuestList(String propertyId, List<ReservationGuestRequestDto> guests) {
        if (!StringUtils.hasText(propertyId)) {
            throw new BadRequestException("propertyId is required");
        }
        if (guests == null || guests.isEmpty()) {
            throw new BadRequestException("At least one reservation guest is required");
        }

        long primaryCount = guests.stream()
                .filter(guest -> guest != null && Boolean.TRUE.equals(guest.getIsPrimary()))
                .count();
        if (primaryCount != 1) {
            throw new BadRequestException("Exactly one reservation guest must be primary");
        }

        Set<Long> requestedProfileIds = new HashSet<>();
        List<Set<String>> newGuestIdentifiers = new ArrayList<>();
        for (ReservationGuestRequestDto guest : guests) {
            if (guest == null || guest.getIsPrimary() == null) {
                throw new BadRequestException("Each reservation guest must specify isPrimary");
            }

            if (guest.getGuestProfileId() != null) {
                if (guest.getGuestProfileId() <= 0) {
                    throw new BadRequestException("guestProfileId must be positive");
                }
                if (!requestedProfileIds.add(guest.getGuestProfileId())) {
                    throw new BadRequestException("The same guestProfileId cannot be supplied more than once");
                }
                continue;
            }

            if (!StringUtils.hasText(guest.getFirstName()) || !StringUtils.hasText(guest.getLastName())) {
                throw new BadRequestException("firstName and lastName are required for a new guest");
            }
            Set<String> identifiers = deterministicIdentifiers(guest);
            if (identifiers.isEmpty()) {
                throw new BadRequestException(
                        "A new guest requires a phone number, mobile number, email, or loyalty number for lookup");
            }
            if (newGuestIdentifiers.stream().anyMatch(existing -> overlaps(existing, identifiers))) {
                throw new BadRequestException(
                        "Duplicate new guest identity data was supplied; include the guest only once");
            }
            newGuestIdentifiers.add(identifiers);
        }
    }

    private GuestLookupRequest toLookupRequest(String propertyId, ReservationGuestRequestDto guest) {
        return GuestLookupRequest.builder()
                .propertyId(propertyId)
                .phoneNumber(guest.getPhoneNumber())
                .mobileNumber(guest.getMobileNumber())
                .personalEmail(guest.getPersonalEmail())
                .officialEmail(guest.getOfficialEmail())
                .loyaltyNumber(guest.getLoyaltyMembershipNumber())
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .build();
    }

    private GuestProfileCreateRequest toCreateRequest(String propertyId, ReservationGuestRequestDto guest) {
        boolean enrollGuest = Boolean.TRUE.equals(guest.getEnrollGuest());
        return GuestProfileCreateRequest.builder()
                .propertyId(propertyId)
                .salutation(guest.getSalutation())
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .personalEmail(guest.getPersonalEmail())
                .officialEmail(guest.getOfficialEmail())
                .phoneNumber(guest.getPhoneNumber())
                .mobileNumber(guest.getMobileNumber())
                .address(guest.getAddress())
                .city(guest.getCity())
                .state(guest.getState())
                .country(guest.getCountry())
                .postalCode(guest.getPostalCode())
                .nationality(guest.getNationality())
                .dateOfBirth(guest.getDateOfBirth())
                .gender(guest.getGender())
                .companyName(guest.getCompanyName())
                .vipStatus(guest.getVipStatus() != null && guest.getVipStatus())
                .idType(guest.getIdType())
                .idNumber(guest.getIdNumber())
                .idDocumentPath(guest.getIdDocumentPath())
                .loyaltyMembershipNumber(enrollGuest ? TEMPORARY_LOYALTY_NUMBER : null)
                .loyaltyTier(enrollGuest ? DEFAULT_LOYALTY_TIER : null)
                .build();
    }

    private Set<String> deterministicIdentifiers(ReservationGuestRequestDto guest) {
        Set<String> identifiers = new LinkedHashSet<>();
        addIdentifier(identifiers, guest.getPhoneNumber());
        addIdentifier(identifiers, guest.getMobileNumber());
        addIdentifier(identifiers, guest.getPersonalEmail());
        addIdentifier(identifiers, guest.getOfficialEmail());
        addIdentifier(identifiers, guest.getLoyaltyMembershipNumber());
        return identifiers;
    }

    private void addIdentifier(Set<String> identifiers, String value) {
        if (StringUtils.hasText(value)) {
            identifiers.add(value.trim().toLowerCase(Locale.ROOT));
        }
    }

    private boolean overlaps(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private void verifyProperty(GuestProfileResponse profile, String propertyId) {
        if (!propertyId.equals(profile.getPropertyId())) {
            throw new BadRequestException("Guest profile does not belong to the requested property");
        }
    }

    private GuestProfileResponse enrollExistingGuestIfRequested(
            ReservationGuestRequestDto request,
            GuestProfileResponse profile
    ) {
        validateLoyaltyEnrollment(request, profile);
        if (!Boolean.TRUE.equals(request.getEnrollGuest())) {
            return profile;
        }

        GuestProfileUpdateRequest updateRequest = GuestProfileUpdateRequest.builder()
                .salutation(profile.getSalutation())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .personalEmail(profile.getPersonalEmail())
                .officialEmail(profile.getOfficialEmail())
                .phoneNumber(profile.getPhoneNumber())
                .mobileNumber(profile.getMobileNumber())
                .address(profile.getAddress())
                .city(profile.getCity())
                .state(profile.getState())
                .country(profile.getCountry())
                .postalCode(profile.getPostalCode())
                .nationality(profile.getNationality())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .companyName(profile.getCompanyName())
                .vipStatus(profile.getVipStatus())
                .idType(profile.getIdType())
                .idNumber(profile.getIdNumber())
                .idDocumentPath(profile.getIdDocumentPath())
                .loyaltyMembershipNumber(TEMPORARY_LOYALTY_NUMBER)
                .loyaltyTier(DEFAULT_LOYALTY_TIER)
                .build();
        GuestProfileResponse updatedProfile =
                guestServiceClient.updateGuestProfile(profile.getId(), updateRequest);
        verifyProperty(updatedProfile, profile.getPropertyId());
        return updatedProfile;
    }

    private void validateLoyaltyEnrollment(
            ReservationGuestRequestDto request,
            GuestProfileResponse profile
    ) {
        boolean hasMembershipNumber = StringUtils.hasText(profile.getLoyaltyMembershipNumber());
        boolean hasLoyaltyTier = StringUtils.hasText(profile.getLoyaltyTier());
        if (hasMembershipNumber != hasLoyaltyTier) {
            throw new BadRequestException(
                    "Guest profile has inconsistent loyalty data; membership number and tier must both be set or both be blank");
        }
        if (Boolean.TRUE.equals(request.getEnrollGuest()) && hasMembershipNumber) {
            throw new BadRequestException(
                    "Guest already has a loyalty membership and cannot be enrolled again");
        }
    }
}
