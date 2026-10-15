package com.pms.guest.service.impl;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import com.pms.guest.entity.GuestProfile;
import com.pms.guest.integration.ReservationServiceClient;
import com.pms.guest.mapper.GuestProfileMapper;
import com.pms.guest.repository.GuestProfileRepository;
import com.pms.guest.service.GuestProfileService;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Creates and manages property-scoped guest profiles, including guest IDs and loyalty data. */
@Service
@Transactional(readOnly = true)
public class GuestProfileServiceImpl implements GuestProfileService {

    private static final int GUEST_ID_GENERATION_ATTEMPTS = 5;

    private final GuestProfileRepository guestProfileRepository;
    private final GuestProfileMapper guestProfileMapper;
    private final ReservationServiceClient reservationServiceClient;

    @Autowired
    public GuestProfileServiceImpl(
            GuestProfileRepository guestProfileRepository,
            GuestProfileMapper guestProfileMapper,
            ReservationServiceClient reservationServiceClient
    ) {
        this.guestProfileRepository = guestProfileRepository;
        this.guestProfileMapper = guestProfileMapper;
        this.reservationServiceClient = reservationServiceClient;
    }

    public GuestProfileServiceImpl(
            GuestProfileRepository guestProfileRepository,
            GuestProfileMapper guestProfileMapper
    ) {
        this(guestProfileRepository, guestProfileMapper, null);
    }

    @Override
    @Transactional
    public GuestProfileResponse createGuestProfile(GuestProfileCreateRequest request) {
        validateLoyalty(request.getLoyaltyMembershipNumber(), request.getLoyaltyTier());
        GuestProfile guestProfile = guestProfileMapper.toEntity(request);
        guestProfile.setGuestId(generateGuestId());
        GuestProfile saved = guestProfileRepository.save(guestProfile);
        return guestProfileMapper.toResponse(saved);
    }

    @Override
    public GuestProfileResponse getGuestProfileById(Long id, String propertyId) {
        return guestProfileMapper.toResponse(findGuestProfile(id, propertyId));
    }

    @Override
    @Transactional
    public GuestProfileResponse updateGuestProfile(
            Long id,
            String propertyId,
            GuestProfileUpdateRequest request
    ) {
        GuestProfile guestProfile = findGuestProfile(id, propertyId);
        String loyaltyMembershipNumber = request.getLoyaltyMembershipNumber() == null
                ? guestProfile.getLoyaltyMembershipNumber()
                : request.getLoyaltyMembershipNumber();
        String loyaltyTier = request.getLoyaltyTier() == null
                ? guestProfile.getLoyaltyTier()
                : request.getLoyaltyTier();
        validateLoyalty(loyaltyMembershipNumber, loyaltyTier);
        guestProfileMapper.updateEntity(request, guestProfile);
        GuestProfile saved = guestProfileRepository.save(guestProfile);
        return guestProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGuestProfile(Long id, String propertyId) {
        GuestProfile guestProfile = findGuestProfile(id, propertyId);
        if (reservationServiceClient == null) {
            throw new IllegalStateException("Reservation service client is required for guest deletion");
        }
        if (!reservationServiceClient.findAssignmentsByGuestProfileIds(propertyId, List.of(id)).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guest profile cannot be deleted while reservation history exists");
        }
        guestProfileRepository.delete(guestProfile);
    }

    @Override
    public Optional<GuestProfileResponse> findExistingGuest(GuestLookupRequest request) {
        String propertyId = request.getPropertyId().trim();
        Map<Long, GuestProfile> matchesById = new LinkedHashMap<>();

        if (StringUtils.hasText(request.getPhoneNumber())) {
            addMatches(matchesById, guestProfileRepository.findByPropertyIdAndPhoneNumber(
                    propertyId, request.getPhoneNumber().trim()));
        }
        if (StringUtils.hasText(request.getMobileNumber())) {
            addMatches(matchesById, guestProfileRepository.findByPropertyIdAndMobileNumber(
                    propertyId, request.getMobileNumber().trim()));
        }
        if (StringUtils.hasText(request.getPersonalEmail())) {
            addMatches(matchesById, guestProfileRepository.findByPropertyIdAndPersonalEmail(
                    propertyId, request.getPersonalEmail().trim()));
        }
        if (StringUtils.hasText(request.getOfficialEmail())) {
            addMatches(matchesById, guestProfileRepository.findByPropertyIdAndOfficialEmail(
                    propertyId, request.getOfficialEmail().trim()));
        }
        if (StringUtils.hasText(request.getLoyaltyNumber())) {
            addMatches(matchesById, guestProfileRepository.findByPropertyIdAndLoyaltyMembershipNumber(
                    propertyId, request.getLoyaltyNumber().trim()));
        }

        List<GuestProfile> candidates = new ArrayList<>(matchesById.values());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (StringUtils.hasText(request.getFirstName()) || StringUtils.hasText(request.getLastName())) {
            List<GuestProfile> nameMatches =
                    guestProfileRepository.findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                            propertyId,
                            StringUtils.hasText(request.getFirstName()) ? request.getFirstName().trim() : "",
                            StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : ""
                    );
            Map<Long, GuestProfile> nameMatchesById = new LinkedHashMap<>();
            addMatches(nameMatchesById, nameMatches);
            candidates.removeIf(candidate -> !nameMatchesById.containsKey(candidate.getId()));
        }

        if (candidates.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple guest profiles match the supplied lookup criteria");
        }
        return candidates.stream().findFirst().map(guestProfileMapper::toResponse);
    }

    @Override
    public List<GuestProfileResponse> searchGuestProfiles(
            String propertyId,
            String firstName,
            String lastName,
            String phoneNumber,
            String personalEmail,
            String officialEmail,
            String loyaltyMembershipNumber
    ) {
        if (!StringUtils.hasText(propertyId)) {
            throw new IllegalArgumentException("propertyId is required");
        }

        String scopedPropertyId = propertyId.trim();
        List<List<GuestProfile>> criteriaResults = new ArrayList<>();
        boolean hasCriteria = false;

        if (StringUtils.hasText(firstName) || StringUtils.hasText(lastName)) {
            criteriaResults.add(guestProfileRepository
                    .findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                            scopedPropertyId,
                            StringUtils.hasText(firstName) ? firstName.trim() : "",
                            StringUtils.hasText(lastName) ? lastName.trim() : ""
                    ));
            hasCriteria = true;
        }
        if (StringUtils.hasText(phoneNumber)) {
            LinkedHashSet<GuestProfile> phoneMatches = new LinkedHashSet<>(
                    guestProfileRepository.findByPropertyIdAndPhoneNumber(scopedPropertyId, phoneNumber.trim()));
            phoneMatches.addAll(guestProfileRepository.findByPropertyIdAndMobileNumber(
                    scopedPropertyId, phoneNumber.trim()));
            criteriaResults.add(new ArrayList<>(phoneMatches));
            hasCriteria = true;
        }
        if (StringUtils.hasText(personalEmail)) {
            criteriaResults.add(guestProfileRepository.findByPropertyIdAndPersonalEmail(
                    scopedPropertyId, personalEmail.trim()));
            hasCriteria = true;
        }
        if (StringUtils.hasText(officialEmail)) {
            criteriaResults.add(guestProfileRepository.findByPropertyIdAndOfficialEmail(
                    scopedPropertyId, officialEmail.trim()));
            hasCriteria = true;
        }
        if (StringUtils.hasText(loyaltyMembershipNumber)) {
            criteriaResults.add(guestProfileRepository.findByPropertyIdAndLoyaltyMembershipNumber(
                    scopedPropertyId, loyaltyMembershipNumber.trim()));
            hasCriteria = true;
        }

        if (!hasCriteria) {
            return List.of();
        }

        LinkedHashSet<GuestProfile> matches = new LinkedHashSet<>(criteriaResults.get(0));
        for (int i = 1; i < criteriaResults.size(); i++) {
            matches.retainAll(criteriaResults.get(i));
        }

        return matches.stream()
                .map(guestProfileMapper::toResponse)
                .toList();
    }

    private GuestProfile findGuestProfile(Long id, String propertyId) {
        if (!StringUtils.hasText(propertyId)) {
            throw new IllegalArgumentException("propertyId is required");
        }
        return guestProfileRepository.findByIdAndPropertyId(id, propertyId.trim())
                .orElseThrow(() -> new EntityNotFoundException("Guest profile not found: " + id));
    }

    private void validateLoyalty(String membershipNumber, String tier) {
        boolean hasMembershipNumber = StringUtils.hasText(membershipNumber);
        boolean hasTier = StringUtils.hasText(tier);
        if (hasMembershipNumber != hasTier
                || (!hasMembershipNumber && (membershipNumber != null || tier != null))) {
            throw new IllegalArgumentException(
                    "loyaltyMembershipNumber and loyaltyTier must both be null or both be populated");
        }
    }

    private String generateGuestId() {
        for (int attempt = 0; attempt < GUEST_ID_GENERATION_ATTEMPTS; attempt++) {
            // Keep the GST prefix as the guest-ID namespace; UUIDs make IDs unique across profiles.
            String guestId = "GST-" + UUID.randomUUID();
            if (!guestProfileRepository.existsByGuestId(guestId)) {
                return guestId;
            }
        }
        throw new IllegalStateException("Unable to generate a unique guest ID");
    }

    private void addMatches(Map<Long, GuestProfile> matchesById, List<GuestProfile> matches) {
        matches.forEach(match -> matchesById.putIfAbsent(match.getId(), match));
    }
}
