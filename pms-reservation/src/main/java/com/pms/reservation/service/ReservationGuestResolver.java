package com.pms.reservation.service;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.LoyaltyServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileCreateRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.GuestProfileUpdateRequest;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class ReservationGuestResolver {
    
    private static final String GUEST_ID_PREFIX = "GST-";

    private final GuestServiceClient guestServiceClient;
    private final LoyaltyServiceClient loyaltyServiceClient;

    public ReservationGuestResolver(
            GuestServiceClient guestServiceClient,
            LoyaltyServiceClient loyaltyServiceClient
    ) {
        this.guestServiceClient = guestServiceClient;
        this.loyaltyServiceClient = loyaltyServiceClient;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ResolvedReservationGuest> resolveGuests(
            String propertyId,
            List<ReservationGuestRequestDto> guests
    ) {
        validateGuestList(propertyId, guests);

        List<Optional<GuestProfileResponse>> resolvedOrPending = new ArrayList<>(guests.size());
        Set<Long> matchedProfileIds = new HashSet<>();

        for (ReservationGuestRequestDto guest : guests) {
            Optional<GuestProfileResponse> resolved = resolveGuest(propertyId, guest);
            if (resolved.isPresent()) {
                GuestProfileResponse profile = resolved.get();
                if (!matchedProfileIds.add(profile.getId())) {
                    throw new BadRequestException("The same guest profile cannot be assigned more than once");
                }
            }
            resolvedOrPending.add(resolved);
        }

        Set<Long> createdProfileIds = new HashSet<>();
        List<ResolvedReservationGuest> resolved = new ArrayList<>(guests.size());
        for (int index = 0; index < guests.size(); index++) {
            ReservationGuestRequestDto request = guests.get(index);
            Optional<GuestProfileResponse> matchedProfile = resolvedOrPending.get(index);
            GuestProfileResponse profile = matchedProfile.orElseGet(() -> {
                GuestProfileResponse created = guestServiceClient.createGuest(toCreateRequest(propertyId, request));
                verifyProperty(created, propertyId);
                return created;
            });
            profile = applyLoyaltyEnrollment(request, profile);
            if (matchedProfile.isEmpty()
                    && (matchedProfileIds.contains(profile.getId())
                        || !createdProfileIds.add(profile.getId()))) {
                throw new BadRequestException("The same guest profile cannot be assigned more than once");
            }
            resolved.add(new ResolvedReservationGuest(profile.getId(), request.getIsPrimary(),
                    profile.getLoyaltyMembershipNumber()));
        }
        return List.copyOf(resolved);
    }

    private Optional<GuestProfileResponse> resolveGuest(
            String propertyId,
            ReservationGuestRequestDto guest
    ) {
        if (guest.getGuestProfileId() != null) {
            GuestProfileResponse profile = guestServiceClient.getGuestById(guest.getGuestProfileId(), propertyId)
                    .orElseThrow(() -> new BadRequestException(
                            "Guest profile not found: " + guest.getGuestProfileId()));
            verifyProperty(profile, propertyId);
            return Optional.of(profile);
        }

        Optional<GuestProfileResponse> match =
                guestServiceClient.findExistingGuest(toLookupRequest(propertyId, guest));
        if (match.isPresent()) {
            verifyProperty(match.get(), propertyId);
        }
        return match;
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

            validateLoyaltyProgramId(guest);

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
                .build();
    }

    /**
     * Assigns the loyalty membership issued by the loyalty service when the guest is enrolled in membership.
     *
     * A guest that already holds a membership keeps it: the originally assigned loyalty number and tier are
     * never overwritten, so rebooking an enrolled guest cannot reassign them.
     */
    private GuestProfileResponse applyLoyaltyEnrollment(
            ReservationGuestRequestDto request,
            GuestProfileResponse profile
    ) {
        validateLoyaltyDataConsistency(profile);

        if (!Boolean.TRUE.equals(request.getEnrollGuest()) || hasLoyaltyMembership(profile)) {
            return profile;
        }

        UUID loyaltyGuestId = loyaltyGuestId(profile)
                .orElseThrow(() -> new BadRequestException(
                        "Guest profile " + profile.getId() + " cannot be enrolled in membership because it has no "
                                + "usable guest id"));

        LoyaltyMembershipResponse membership = enrollInMembership(request, profile, loyaltyGuestId);

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
                .loyaltyMembershipNumber(membership.getLoyaltyNumber())
                .loyaltyTier(membership.getTierCode())
                .build();

        GuestProfileResponse updatedProfile = guestServiceClient.updateGuestProfile(
                profile.getId(), profile.getPropertyId(), updateRequest);
        verifyProperty(updatedProfile, profile.getPropertyId());
        requireLoyaltyStoredOnProfile(request, updatedProfile, membership);
        return updatedProfile;
    }

    /**
     * Calls the loyalty service, reporting the guest and program the failure belongs to. Without this the
     * booking only reports whatever the loyalty service returned, and the guest profile is left with no
     * membership and no indication of which step failed.
     */
    private LoyaltyMembershipResponse enrollInMembership(
            ReservationGuestRequestDto request,
            GuestProfileResponse profile,
            UUID loyaltyGuestId
    ) {
        Optional<UUID> selectedProgram = selectedLoyaltyProgram(request);
        try {
            return selectedProgram
                    .map(programId -> loyaltyServiceClient.ensureMembership(loyaltyGuestId, programId))
                    .orElseGet(() -> loyaltyServiceClient.ensureMembership(loyaltyGuestId));
        } catch (RuntimeException ex) {
            // A missing program configuration is a request/config problem, not a loyalty service failure.
            if (ex instanceof BadRequestException) {
                throw ex;
            }
            String program = selectedProgram.map(UUID::toString).orElse("the configured default program");
            throw new ExternalServiceException(
                    "Loyalty enrollment failed for guest profile " + profile.getId() + " ("
                            + profile.getFirstName() + " " + profile.getLastName() + ", guest id " + profile.getGuestId()
                            + ") in " + program + ": " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Confirms the membership actually reached the guest profile. A successful enrollment whose update does
     * not persist the number and tier would otherwise leave the guest silently unenrolled.
     */
    private void requireLoyaltyStoredOnProfile(
            ReservationGuestRequestDto request,
            GuestProfileResponse updatedProfile,
            LoyaltyMembershipResponse membership
    ) {
        boolean hasLoyaltyNumber = StringUtils.hasText(updatedProfile.getLoyaltyMembershipNumber());
        boolean hasLoyaltyTier = StringUtils.hasText(updatedProfile.getLoyaltyTier());
        if (hasLoyaltyNumber && hasLoyaltyTier) {
            return;
        }

        List<String> missing = new ArrayList<>(2);
        if (!hasLoyaltyNumber) {
            missing.add("loyaltyMembershipNumber");
        }
        if (!hasLoyaltyTier) {
            missing.add("loyaltyTier");
        }

        throw new ExternalServiceException(
                "Loyalty membership was issued for guest profile " + updatedProfile.getId() + " (number "
                        + membership.getLoyaltyNumber() + ", tier " + membership.getTierCode()
                        + ") but the guest service did not store " + String.join(" and ", missing)
                        + " on the profile, so " + (request.getIsPrimary() ? "the primary guest" : "the guest")
                        + " remains without a membership");
    }

    private boolean hasLoyaltyMembership(GuestProfileResponse profile) {
        return StringUtils.hasText(profile.getLoyaltyMembershipNumber())
                && StringUtils.hasText(profile.getLoyaltyTier());
    }

    private void validateLoyaltyDataConsistency(GuestProfileResponse profile) {
        boolean hasMembershipNumber = StringUtils.hasText(profile.getLoyaltyMembershipNumber());
        boolean hasLoyaltyTier = StringUtils.hasText(profile.getLoyaltyTier());
        if (hasMembershipNumber != hasLoyaltyTier) {
            throw new BadRequestException(
                    "Guest profile has inconsistent loyalty data; membership number and tier must both be set or both be blank");
        }
    }

    /**
     * Rejects an unusable loyalty program before any guest is looked up or created, so a malformed
     * selection fails the booking with a client error instead of after external calls have been made.
     */
    private void validateLoyaltyProgramId(ReservationGuestRequestDto guest) {
        if (!StringUtils.hasText(guest.getLoyaltyProgramId())) {
            return;
        }

        String candidate = guest.getLoyaltyProgramId().trim();
        try {
            UUID.fromString(candidate);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("loyaltyProgramId must be a valid UUID: " + candidate);
        }
    }

    /**
     * The loyalty program the caller picked for this guest, or empty to fall back to the program
     * configured for the property. A hotel chain offering several programs uses this to enrol the guest
     * in the chosen one and have a tier drawn from that program's active tiers.
     */
    private Optional<UUID> selectedLoyaltyProgram(ReservationGuestRequestDto request) {
        if (!StringUtils.hasText(request.getLoyaltyProgramId())) {
            return Optional.empty();
        }

        return Optional.of(UUID.fromString(request.getLoyaltyProgramId().trim()));
    }

    /**
     * Loyalty memberships are keyed by the UUID carried in the guest profile id.
     */
    private Optional<UUID> loyaltyGuestId(GuestProfileResponse profile) {
        if (!StringUtils.hasText(profile.getGuestId())) {
            return Optional.empty();
        }
        String candidate = profile.getGuestId().trim();
        if (candidate.regionMatches(true, 0, GUEST_ID_PREFIX, 0, GUEST_ID_PREFIX.length())) {
            candidate = candidate.substring(GUEST_ID_PREFIX.length());
        }
        try {
            return Optional.of(UUID.fromString(candidate));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
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
}
