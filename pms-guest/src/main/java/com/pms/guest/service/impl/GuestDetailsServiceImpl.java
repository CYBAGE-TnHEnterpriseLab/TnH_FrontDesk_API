package com.pms.guest.service.impl;

import com.pms.guest.dto.response.GuestDetailsResponse;
import com.pms.guest.dto.response.GuestDetailsResponse.GuestAssignmentDetails;
import com.pms.guest.dto.response.GuestDetailsResponse.GuestDetails;
import com.pms.guest.dto.response.GuestDetailsResponse.Membership;
import com.pms.guest.entity.GuestProfile;
import com.pms.guest.integration.ReservationServiceClient;
import com.pms.guest.integration.dto.ReservationGuestAssignment;
import com.pms.guest.repository.GuestProfileRepository;
import com.pms.guest.service.GuestDetailsService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Combines guest profiles with reservation assignments for guest-details responses. */
@Service
public class GuestDetailsServiceImpl implements GuestDetailsService {

    private final GuestProfileRepository guestProfileRepository;
    private final ReservationServiceClient reservationServiceClient;

    public GuestDetailsServiceImpl(
            GuestProfileRepository guestProfileRepository,
            ReservationServiceClient reservationServiceClient
    ) {
        this.guestProfileRepository = guestProfileRepository;
        this.reservationServiceClient = reservationServiceClient;
    }

    @Override
    public GuestDetailsResponse getGuestDetails(
            String propertyId,
            String phoneNumber,
            String email,
            Long bookingId,
            String confirmationNumber
    ) {
        if (!StringUtils.hasText(propertyId)) {
            throw new IllegalArgumentException("propertyId is required");
        }
        int criteria = (StringUtils.hasText(phoneNumber) ? 1 : 0)
                + (StringUtils.hasText(email) ? 1 : 0)
                + (bookingId != null ? 1 : 0)
                + (StringUtils.hasText(confirmationNumber) ? 1 : 0);
        if (criteria != 1) {
            throw new IllegalArgumentException(
                    "Exactly one of phoneNumber, email, bookingId or confirmationNumber is required");
        }
        if (bookingId != null && bookingId <= 0) {
            throw new IllegalArgumentException("bookingId must be positive");
        }

        String scopedPropertyId = propertyId.trim();
        GuestDetailsResponse.GuestDetailsResponseBuilder response = GuestDetailsResponse.builder()
                .propertyId(scopedPropertyId);
        List<GuestAssignmentDetails> guests;

        if (StringUtils.hasText(phoneNumber)) {
            String phone = phoneNumber.trim();
            guests = fromProfiles(scopedPropertyId, mergeById(
                    guestProfileRepository.findByPropertyIdAndPhoneNumber(scopedPropertyId, phone),
                    guestProfileRepository.findByPropertyIdAndMobileNumber(scopedPropertyId, phone)));
        } else if (StringUtils.hasText(email)) {
            // Exact, case-sensitive equality: casing is intentionally not normalized.
            String exactEmail = email.trim();
            guests = fromProfiles(scopedPropertyId, mergeById(
                    guestProfileRepository.findByPropertyIdAndPersonalEmail(scopedPropertyId, exactEmail),
                    guestProfileRepository.findByPropertyIdAndOfficialEmail(scopedPropertyId, exactEmail)));
        } else if (bookingId != null) {
            guests = fromAssignments(scopedPropertyId,
                    reservationServiceClient.findAssignmentsByBookingId(scopedPropertyId, bookingId).stream()
                            .filter(assignment -> bookingId.equals(assignment.getBookingId()))
                            .toList());
        } else {
            String confirmation = confirmationNumber.trim();
            guests = fromAssignments(scopedPropertyId,
                    reservationServiceClient.findAssignmentsByConfirmationNumber(scopedPropertyId, confirmation));
        }
        return response.guests(guests).build();
    }

    private List<GuestAssignmentDetails> fromProfiles(String propertyId, List<GuestProfile> profiles) {
        if (profiles.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ReservationGuestAssignment>> assignmentsByProfile = reservationServiceClient
                .findAssignmentsByGuestProfileIds(
                        propertyId,
                        profiles.stream().map(GuestProfile::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        ReservationGuestAssignment::getGuestProfileId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<GuestAssignmentDetails> result = new ArrayList<>();
        for (GuestProfile profile : profiles) {
            List<ReservationGuestAssignment> assignments = assignmentsByProfile.getOrDefault(profile.getId(), List.of());
            if (assignments.isEmpty()) {
                // Matched profile without any reservation assignment: no booking context available.
                result.add(toDetails(null, profile.getId(), profile));
            } else {
                assignments.stream()
                        .sorted(Comparator.comparing(ReservationGuestAssignment::getBookingId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .forEach(assignment -> result.add(toDetails(assignment, profile.getId(), profile)));
            }
        }
        return result;
    }

    private List<GuestAssignmentDetails> fromAssignments(
            String propertyId,
            List<ReservationGuestAssignment> assignments
    ) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> profileIds = assignments.stream()
                .map(ReservationGuestAssignment::getGuestProfileId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GuestProfile> profilesById = guestProfileRepository
                .findByPropertyIdAndIdIn(propertyId, profileIds).stream()
                .collect(Collectors.toMap(GuestProfile::getId, profile -> profile, (a, b) -> a));

        // Each assignment is kept separately so a guest on several bookings appears once per booking.
        return assignments.stream()
                .map(assignment -> toDetails(
                        assignment,
                        assignment.getGuestProfileId(),
                        profilesById.get(assignment.getGuestProfileId())))
                .toList();
    }

    private GuestAssignmentDetails toDetails(
            ReservationGuestAssignment assignment,
            Long guestProfileId,
            GuestProfile profile
    ) {
        return GuestAssignmentDetails.builder()
                .bookingId(assignment == null ? null : assignment.getBookingId())
                .confirmationNumber(assignment == null ? null : assignment.getConfirmationNumber())
                .guestProfileId(guestProfileId)
                .isPrimary(assignment == null ? null : assignment.getIsPrimary())
                .guest(profile == null ? null : toGuest(profile))
                .membership(profile == null ? null : toMembership(profile))
                .build();
    }

    private GuestDetails toGuest(GuestProfile profile) {
        return GuestDetails.builder()
                .guestId(profile.getGuestId())
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
                .build();
    }

    private Membership toMembership(GuestProfile profile) {
        boolean enrolled = StringUtils.hasText(profile.getLoyaltyMembershipNumber())
                && StringUtils.hasText(profile.getLoyaltyTier());
        return Membership.builder()
                .enrolled(enrolled)
                .membershipNumber(enrolled ? profile.getLoyaltyMembershipNumber() : null)
                .tier(enrolled ? profile.getLoyaltyTier() : null)
                .build();
    }

    private List<GuestProfile> mergeById(List<GuestProfile> first, List<GuestProfile> second) {
        Map<Long, GuestProfile> merged = new LinkedHashMap<>();
        first.forEach(profile -> merged.putIfAbsent(profile.getId(), profile));
        second.forEach(profile -> merged.putIfAbsent(profile.getId(), profile));
        return merged.values().stream()
                .sorted(Comparator.comparing(GuestProfile::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
