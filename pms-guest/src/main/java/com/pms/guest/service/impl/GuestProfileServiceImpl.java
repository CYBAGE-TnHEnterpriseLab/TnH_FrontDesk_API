package com.pms.guest.service.impl;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import com.pms.guest.entity.GuestProfile;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class GuestProfileServiceImpl implements GuestProfileService {

    private final GuestProfileRepository guestProfileRepository;
    private final GuestProfileMapper guestProfileMapper;

    public GuestProfileServiceImpl(
            GuestProfileRepository guestProfileRepository,
            GuestProfileMapper guestProfileMapper
    ) {
        this.guestProfileRepository = guestProfileRepository;
        this.guestProfileMapper = guestProfileMapper;
    }

    @Override
    @Transactional
    public GuestProfileResponse createGuestProfile(GuestProfileCreateRequest request) {
        GuestProfile guestProfile = guestProfileMapper.toEntity(request);
        guestProfile.setGuestId("GST-" + UUID.randomUUID());
        GuestProfile saved = guestProfileRepository.save(guestProfile);
        return guestProfileMapper.toResponse(saved);
    }

    @Override
    public GuestProfileResponse getGuestProfileById(Long id) {
        return guestProfileMapper.toResponse(findGuestProfile(id));
    }

    @Override
    @Transactional
    public GuestProfileResponse updateGuestProfile(Long id, GuestProfileUpdateRequest request) {
        GuestProfile guestProfile = findGuestProfile(id);
        guestProfileMapper.updateEntity(request, guestProfile);
        GuestProfile saved = guestProfileRepository.save(guestProfile);
        return guestProfileMapper.toResponse(saved);
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
        if (candidates.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple guest profiles match the supplied lookup criteria");
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
            criteriaResults.add(guestProfileRepository.findByPropertyIdAndPhoneNumber(
                    scopedPropertyId, phoneNumber.trim()));
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

    private GuestProfile findGuestProfile(Long id) {
        return guestProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Guest profile not found: " + id));
    }

    private void addMatches(Map<Long, GuestProfile> matchesById, List<GuestProfile> matches) {
        matches.forEach(match -> matchesById.putIfAbsent(match.getId(), match));
    }
}
