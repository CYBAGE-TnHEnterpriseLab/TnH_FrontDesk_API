package com.pms.guest.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.entity.GuestProfile;
import com.pms.guest.integration.ReservationServiceClient;
import com.pms.guest.mapper.GuestProfileMapper;
import com.pms.guest.repository.GuestProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestProfileServiceImplTest {

    private static final Pattern GENERATED_GUEST_ID =
            Pattern.compile("^GST-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Mock
    private GuestProfileRepository guestProfileRepository;

    private GuestProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuestProfileServiceImpl(guestProfileRepository, new GuestProfileMapper());
    }

    @Test
    void springSelectsConstructorWithReservationServiceClient() {
        GuestProfileMapper mapper = new GuestProfileMapper();
        ReservationServiceClient reservationClient = mock(ReservationServiceClient.class);
        GuestProfile profile = new GuestProfile();
        profile.setId(27L);
        profile.setPropertyId("PROP001");
        when(guestProfileRepository.findByIdAndPropertyId(27L, "PROP001"))
                .thenReturn(Optional.of(profile));
        when(reservationClient.findAssignmentsByGuestProfileIds("PROP001", List.of(27L)))
                .thenReturn(List.of());

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(GuestProfileRepository.class, () -> guestProfileRepository);
            context.registerBean(GuestProfileMapper.class, () -> mapper);
            context.registerBean(ReservationServiceClient.class, () -> reservationClient);
            context.register(GuestProfileServiceImpl.class);
            context.refresh();

            context.getBean(GuestProfileServiceImpl.class).deleteGuestProfile(27L, "PROP001");

            verify(reservationClient).findAssignmentsByGuestProfileIds("PROP001", List.of(27L));
            verify(guestProfileRepository).delete(profile);
        }
    }

    @Test
    void createMapsRequestGeneratesUniqueBusinessIdAndPersistsProfile() {
        GuestProfileCreateRequest request = new GuestProfileCreateRequest();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr");
        request.setFirstName("Manish");
        request.setLastName("Das");
        request.setPersonalEmail("manish.das@example.com");
        request.setPhoneNumber("+1-555-0101");
        request.setAddress("42 Business Park");
        request.setCity("New York");
        request.setState("NY");
        request.setCountry("USA");
        request.setPostalCode("10001");
        request.setNationality("American");
        request.setVipStatus(false);
        request.setIdDocumentPath("/uploads/guests/P1234567.pdf");

        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> {
            GuestProfile profile = invocation.getArgument(0);
            profile.setId(27L);
            return profile;
        });

        var response = service.createGuestProfile(request);

        ArgumentCaptor<GuestProfile> savedProfile = ArgumentCaptor.forClass(GuestProfile.class);
        verify(guestProfileRepository).save(savedProfile.capture());
        GuestProfile profile = savedProfile.getValue();
        assertThat(profile.getId()).isEqualTo(27L);
        assertThat(profile.getGuestId()).matches(GENERATED_GUEST_ID);
        assertThat(profile.getPropertyId()).isEqualTo("PROP001");
        assertThat(profile.getFirstName()).isEqualTo("Manish");
        assertThat(profile.getLastName()).isEqualTo("Das");
        assertThat(profile.getVipStatus()).isFalse();
        assertThat(profile.getIdDocumentPath()).isEqualTo("/uploads/guests/P1234567.pdf");
        assertThat(profile.getLoyaltyMembershipNumber()).isNull();
        assertThat(profile.getLoyaltyTier()).isNull();
        assertThat(response.getId()).isEqualTo(27L);
        assertThat(response.getGuestId()).isEqualTo(profile.getGuestId());
        assertThat(response.getPropertyId()).isEqualTo("PROP001");
        assertThat(response.getFirstName()).isEqualTo("Manish");
        assertThat(response.getLastName()).isEqualTo("Das");
    }

    @Test
    void createRetriesWhenGeneratedBusinessIdAlreadyExists() {
        GuestProfileCreateRequest request = new GuestProfileCreateRequest();
        request.setPropertyId("PROP001");
        request.setFirstName("Manish");
        request.setLastName("Das");

        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(true, false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createGuestProfile(request);

        verify(guestProfileRepository, times(2)).existsByGuestId(anyString());
        verify(guestProfileRepository).save(any(GuestProfile.class));
    }

    @Test
    void createPersistsAndReturnsUploadedDocumentReference() {
        GuestProfileCreateRequest request = createRequest();
        request.setIdDocumentPath("uploads/id-document.png");
        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> {
            GuestProfile profile = invocation.getArgument(0);
            profile.setId(31L);
            return profile;
        });

        var response = service.createGuestProfile(request);

        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getIdDocumentPath()).isEqualTo("uploads/id-document.png");
        verify(guestProfileRepository).save(org.mockito.ArgumentMatchers.argThat(
                profile -> "uploads/id-document.png".equals(profile.getIdDocumentPath())));
    }

    @Test
    void createDefaultsVipStatusToFalse() {
        GuestProfileCreateRequest request = createRequest();
        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createGuestProfile(request);

        assertThat(response.getVipStatus()).isFalse();
    }

    @Test
    void createRejectsIncompleteLoyaltyPairBeforeSaving() {
        GuestProfileCreateRequest request = createRequest();
        request.setLoyaltyMembershipNumber("LOYALTY-1");

        assertThatThrownBy(() -> service.createGuestProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must both be null or both be populated");
        verify(guestProfileRepository, never()).save(any());
    }

    @Test
    void createAcceptsBothValidLoyaltyStates() {
        when(guestProfileRepository.existsByGuestId(anyString())).thenReturn(false);
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createGuestProfile(createRequest());

        GuestProfileCreateRequest enrolled = createRequest();
        enrolled.setLoyaltyMembershipNumber("LOYALTY-1");
        enrolled.setLoyaltyTier("GOLD");
        service.createGuestProfile(enrolled);

        verify(guestProfileRepository, times(2)).save(any(GuestProfile.class));
    }

    @Test
    void getProfileIsPropertyScopedAndReturnsNotFoundForAnotherProperty() {
        GuestProfile profile = profile(42L, "PROP-A");
        when(guestProfileRepository.findByIdAndPropertyId(42L, "PROP-A"))
                .thenReturn(Optional.of(profile));

        assertThat(service.getGuestProfileById(42L, "PROP-A").getGuestId()).isEqualTo("GST-42");
        when(guestProfileRepository.findByIdAndPropertyId(42L, "PROP-B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGuestProfileById(42L, "PROP-B"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updatePreservesUnprovidedContactLoyaltyAndDocumentFields() {
        GuestProfile profile = profile(42L, "PROP-A");
        profile.setPersonalEmail("personal@example.com");
        profile.setOfficialEmail("official@example.com");
        profile.setPhoneNumber("555-1000");
        profile.setMobileNumber("555-2000");
        profile.setLoyaltyMembershipNumber("LOYALTY-1");
        profile.setLoyaltyTier("GOLD");
        profile.setIdDocumentPath("uploads/id.png");
        profile.setCreatedBy(java.util.UUID.randomUUID());
        var createdBy = profile.getCreatedBy();
        GuestProfileUpdateRequest request = new GuestProfileUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Guest");
        when(guestProfileRepository.findByIdAndPropertyId(42L, "PROP-A")).thenReturn(Optional.of(profile));
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateGuestProfile(42L, "PROP-A", request);

        assertThat(response.getFirstName()).isEqualTo("Updated");
        assertThat(response.getPersonalEmail()).isEqualTo("personal@example.com");
        assertThat(response.getOfficialEmail()).isEqualTo("official@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("555-1000");
        assertThat(response.getMobileNumber()).isEqualTo("555-2000");
        assertThat(response.getLoyaltyMembershipNumber()).isEqualTo("LOYALTY-1");
        assertThat(response.getLoyaltyTier()).isEqualTo("GOLD");
        assertThat(response.getIdDocumentPath()).isEqualTo("uploads/id.png");
        assertThat(profile.getCreatedBy()).isEqualTo(createdBy);
        assertThat(profile.getGuestId()).isEqualTo("GST-42");
        assertThat(profile.getPropertyId()).isEqualTo("PROP-A");
    }

    @Test
    void updateAllowsOmittingNamesAndPreservesEveryUnprovidedProfileField() {
        GuestProfile profile = profile(42L, "PROP-A");
        profile.setFirstName("Original");
        profile.setLastName("Guest");
        profile.setPersonalEmail("personal@example.com");
        GuestProfileUpdateRequest request = new GuestProfileUpdateRequest();
        request.setPersonalEmail("updated@example.com");
        when(guestProfileRepository.findByIdAndPropertyId(42L, "PROP-A")).thenReturn(Optional.of(profile));
        when(guestProfileRepository.save(any(GuestProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateGuestProfile(42L, "PROP-A", request);

        assertThat(response.getFirstName()).isEqualTo("Original");
        assertThat(response.getLastName()).isEqualTo("Guest");
        assertThat(response.getPersonalEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void updateRejectsIncompleteResultingLoyaltyPair() {
        GuestProfile profile = profile(42L, "PROP-A");
        profile.setLoyaltyMembershipNumber("LOYALTY-1");
        profile.setLoyaltyTier("GOLD");
        GuestProfileUpdateRequest request = updateRequest();
        request.setLoyaltyMembershipNumber(" ");
        when(guestProfileRepository.findByIdAndPropertyId(42L, "PROP-A")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateGuestProfile(42L, "PROP-A", request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(guestProfileRepository, never()).save(any());
    }

    @Test
    void searchUsesExactPropertyScopedContactAndLoyaltyFilters() {
        GuestProfile phoneMatch = profile(1L, "PROP-A");
        GuestProfile emailMatch = profile(2L, "PROP-A");
        GuestProfile loyaltyMatch = profile(3L, "PROP-A");
        GuestProfile officialEmailMatch = profile(4L, "PROP-A");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber("PROP-A", "555-1000"))
                .thenReturn(List.of(phoneMatch));
        when(guestProfileRepository.findByPropertyIdAndMobileNumber("PROP-A", "555-1000"))
                .thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail("PROP-A", "a@example.com"))
                .thenReturn(List.of(emailMatch));
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail("PROP-A", "a@work.example"))
                .thenReturn(List.of(officialEmailMatch));
        when(guestProfileRepository.findByPropertyIdAndLoyaltyMembershipNumber("PROP-A", "LOYALTY-3"))
                .thenReturn(List.of(loyaltyMatch));

        assertThat(service.searchGuestProfiles(" PROP-A ", null, null, "555-1000", null, null, null))
                .extracting(response -> response.getId()).containsExactly(1L);
        assertThat(service.searchGuestProfiles("PROP-A", null, null, null, "a@example.com", null, null))
                .extracting(response -> response.getId()).containsExactly(2L);
        assertThat(service.searchGuestProfiles("PROP-A", null, null, null, null, "a@work.example", null))
                .extracting(response -> response.getId()).containsExactly(4L);
        assertThat(service.searchGuestProfiles("PROP-A", null, null, null, null, null, "LOYALTY-3"))
                .extracting(response -> response.getId()).containsExactly(3L);
        verify(guestProfileRepository, never()).findByPropertyIdAndPhoneNumber("PROP-B", "555-1000");
    }

    @Test
    void searchSupportsNameFiltersAndNoCriteriaReturnsNoResults() {
        GuestProfile match = profile(9L, "PROP-A");
        when(guestProfileRepository.findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                "PROP-A", "ava", "guest")).thenReturn(List.of(match));

        assertThat(service.searchGuestProfiles("PROP-A", "ava", "guest", null, null, null, null))
                .extracting(response -> response.getId()).containsExactly(9L);
        assertThat(service.searchGuestProfiles("PROP-A", null, null, null, null, null, null)).isEmpty();
    }

    @Test
    void lookupReturnsOnePropertyScopedDeterministicMatch() {
        GuestProfile match = profile(42L, "PROP-A");
        GuestLookupRequest request = lookupRequest("PROP-A", "555-1000");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber("PROP-A", "555-1000"))
                .thenReturn(List.of(match));

        assertThat(service.findExistingGuest(request)).map(response -> response.getId()).contains(42L);
    }

    @Test
    void lookupReturnsEmptyWhenNoIdentifiersMatchAndNeverSearchesOtherProperties() {
        GuestLookupRequest request = lookupRequest("PROP-A", "555-1000");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber("PROP-A", "555-1000"))
                .thenReturn(List.of());

        assertThat(service.findExistingGuest(request)).isEmpty();
        verify(guestProfileRepository, never()).findByPropertyIdAndPhoneNumber("PROP-B", "555-1000");
    }

    @Test
    void lookupReturnsConflictForMultipleMatchesButAllowsNameToNarrowCandidates() {
        GuestProfile first = profile(1L, "PROP-A");
        GuestProfile second = profile(2L, "PROP-A");
        GuestLookupRequest ambiguous = lookupRequest("PROP-A", "555-1000");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber("PROP-A", "555-1000"))
                .thenReturn(List.of(first, second));
        assertThatThrownBy(() -> service.findExistingGuest(ambiguous))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Multiple guest profiles");

        ambiguous.setFirstName("Ava");
        ambiguous.setLastName("Guest");
        when(guestProfileRepository.findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                "PROP-A", "Ava", "Guest")).thenReturn(List.of(second));
        assertThat(service.findExistingGuest(ambiguous)).map(response -> response.getId()).contains(2L);
    }

    @Test
    void nameAloneDoesNotQueryForOrIdentifyAProfile() {
        GuestLookupRequest request = new GuestLookupRequest();
        request.setPropertyId("PROP-A");
        request.setFirstName("Ava");
        request.setLastName("Guest");

        assertThat(service.findExistingGuest(request)).isEmpty();
        verify(guestProfileRepository, never())
                .findByPropertyIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
                        anyString(), anyString(), anyString());
    }

    private GuestProfileCreateRequest createRequest() {
        GuestProfileCreateRequest request = new GuestProfileCreateRequest();
        request.setPropertyId("PROP-A");
        request.setFirstName("Ava");
        request.setLastName("Guest");
        return request;
    }

    private GuestLookupRequest lookupRequest(String propertyId, String phoneNumber) {
        GuestLookupRequest request = new GuestLookupRequest();
        request.setPropertyId(propertyId);
        request.setPhoneNumber(phoneNumber);
        return request;
    }

    private GuestProfileUpdateRequest updateRequest() {
        GuestProfileUpdateRequest request = new GuestProfileUpdateRequest();
        request.setFirstName("Ava");
        request.setLastName("Guest");
        return request;
    }

    private GuestProfile profile(Long id, String propertyId) {
        return GuestProfile.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId(propertyId)
                .firstName("Ava")
                .lastName("Guest")
                .vipStatus(false)
                .build();
    }
}
