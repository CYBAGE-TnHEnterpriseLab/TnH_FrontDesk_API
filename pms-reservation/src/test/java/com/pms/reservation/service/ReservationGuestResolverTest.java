package com.pms.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.LoyaltyServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileCreateRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.GuestProfileUpdateRequest;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationGuestResolverTest {

    private static final UUID LOYALTY_GUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private GuestServiceClient guestServiceClient;

    @Mock
    private LoyaltyServiceClient loyaltyServiceClient;

    private ReservationGuestResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ReservationGuestResolver(guestServiceClient, loyaltyServiceClient);
    }

    @Test
    void resolvesExistingGuestByIdAndRejectsDifferentProperty() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(profile(42L, "PROP001")));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient, never()).findExistingGuest(any());
        verify(guestServiceClient, never()).createGuest(any());

        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(profile(42L, "PROP002")));
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void reusesGuestFoundByDeterministicLookup() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setLoyaltyMembershipNumber("LOYALTY-9");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(profile(42L, "PROP001")));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        ArgumentCaptor<GuestLookupRequest> lookupRequest = ArgumentCaptor.forClass(GuestLookupRequest.class);
        verify(guestServiceClient).findExistingGuest(lookupRequest.capture());
        assertThat(lookupRequest.getValue().getLoyaltyNumber()).isEqualTo("LOYALTY-9");
        verify(guestServiceClient, never()).createGuest(any());
    }

    @Test
    void createsNewGuestWithoutLoyaltyValuesOnTheCreateRequest() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(profile(42L, "PROP001"));

        resolver.resolveGuests("PROP001", List.of(request));

        ArgumentCaptor<GuestProfileCreateRequest> createRequest =
                ArgumentCaptor.forClass(GuestProfileCreateRequest.class);
        verify(guestServiceClient).createGuest(createRequest.capture());
        assertThat(createRequest.getValue().getPropertyId()).isEqualTo("PROP001");
        assertThat(createRequest.getValue().getFirstName()).isEqualTo("Ava");
        assertThat(createRequest.getValue().getVipStatus()).isFalse();
        assertThat(createRequest.getValue().getLoyaltyMembershipNumber()).isNull();
        assertThat(createRequest.getValue().getLoyaltyTier()).isNull();
    }

    @Test
    void createsNewGuestWithNullLoyaltyFieldsWhenNotEnrolled() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(profile(42L, "PROP001"));

        resolver.resolveGuests("PROP001", List.of(request));

        ArgumentCaptor<GuestProfileCreateRequest> createRequest =
                ArgumentCaptor.forClass(GuestProfileCreateRequest.class);
        verify(guestServiceClient).createGuest(createRequest.capture());
        assertThat(createRequest.getValue().getLoyaltyMembershipNumber()).isNull();
        assertThat(createRequest.getValue().getLoyaltyTier()).isNull();
    }

    @Test
    void newEnrolledGuestReceivesLoyaltyNumberAndTierIssuedByLoyaltyService() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(enrollableProfile(42L, "PROP001", null, null));
        when(loyaltyServiceClient.ensureMembership(LOYALTY_GUEST_ID))
                .thenReturn(membership("LOY00000042", "SILVER", "Silver", 1));
        when(guestServiceClient.updateGuestProfile(eq(42L), eq("PROP001"), any(GuestProfileUpdateRequest.class)))
                .thenReturn(enrollableProfile(42L, "PROP001", "LOY00000042", "SILVER"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true, "LOY00000042"));
        ArgumentCaptor<GuestProfileUpdateRequest> updateRequest =
                ArgumentCaptor.forClass(GuestProfileUpdateRequest.class);
        verify(guestServiceClient).updateGuestProfile(
                eq(42L),
                eq("PROP001"),
                updateRequest.capture());
        assertThat(updateRequest.getValue().getLoyaltyMembershipNumber()).isEqualTo("LOY00000042");
        assertThat(updateRequest.getValue().getLoyaltyTier()).isEqualTo("SILVER");
    }

    @Test
    void enrollsExistingGuestWhenMembershipAndTierAreBlankAndPreservesProfileFields() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse existingProfile = completeProfile(42L, "PROP001", null, null);
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(existingProfile));
        when(loyaltyServiceClient.ensureMembership(LOYALTY_GUEST_ID))
                .thenReturn(membership("LOY00000042", "GOLD", "Gold", 2));
        when(guestServiceClient.updateGuestProfile(any(), any(), any(GuestProfileUpdateRequest.class)))
                .thenReturn(completeProfile(42L, "PROP001", "LOY00000042", "GOLD"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true, "LOY00000042"));
        ArgumentCaptor<GuestProfileUpdateRequest> updateRequest =
                ArgumentCaptor.forClass(GuestProfileUpdateRequest.class);
        verify(guestServiceClient).updateGuestProfile(
                eq(42L),
                eq("PROP001"),
                updateRequest.capture());
        assertThat(updateRequest.getValue().getLoyaltyMembershipNumber()).isEqualTo("LOY00000042");
        assertThat(updateRequest.getValue().getLoyaltyTier()).isEqualTo("GOLD");
        assertThat(updateRequest.getValue().getSalutation()).isEqualTo("Ms");
        assertThat(updateRequest.getValue().getFirstName()).isEqualTo("Ava");
        assertThat(updateRequest.getValue().getLastName()).isEqualTo("Guest");
        assertThat(updateRequest.getValue().getPersonalEmail()).isEqualTo("ava@example.com");
        assertThat(updateRequest.getValue().getOfficialEmail()).isEqualTo("ava@work.example");
        assertThat(updateRequest.getValue().getPhoneNumber()).isEqualTo("5551000");
        assertThat(updateRequest.getValue().getMobileNumber()).isEqualTo("5552000");
        assertThat(updateRequest.getValue().getAddress()).isEqualTo("1 Main Street");
        assertThat(updateRequest.getValue().getCity()).isEqualTo("New York");
        assertThat(updateRequest.getValue().getState()).isEqualTo("NY");
        assertThat(updateRequest.getValue().getCountry()).isEqualTo("USA");
        assertThat(updateRequest.getValue().getPostalCode()).isEqualTo("10001");
        assertThat(updateRequest.getValue().getNationality()).isEqualTo("American");
        assertThat(updateRequest.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(updateRequest.getValue().getGender()).isEqualTo("Female");
        assertThat(updateRequest.getValue().getCompanyName()).isEqualTo("Example Inc.");
        assertThat(updateRequest.getValue().getVipStatus()).isTrue();
        assertThat(updateRequest.getValue().getIdType()).isEqualTo("PASSPORT");
        assertThat(updateRequest.getValue().getIdNumber()).isEqualTo("P1234567");
        assertThat(updateRequest.getValue().getIdDocumentPath()).isEqualTo("uploads/id.png");
    }

    @Test
    void rejectsInvalidPrimaryCountDuplicateIdsAndDuplicateNewIdentity() {
        ReservationGuestRequestDto primary = guest(null, true, "Ava", "Guest", "5551000");
        ReservationGuestRequestDto nonPrimary = guest(null, false, "Bea", "Guest", "5552000");
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(primary, primary)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Exactly one");
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(nonPrimary, nonPrimary)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Exactly one");

        ReservationGuestRequestDto existingA = guest(42L, true, "Ava", "Guest", "5551000");
        ReservationGuestRequestDto existingB = guest(42L, false, "Bea", "Guest", "5552000");
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(existingA, existingB)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("same guestProfileId");

        ReservationGuestRequestDto duplicateNew = guest(null, false, "Ava", "Other", "5551000");
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(primary, duplicateNew)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate new guest");

        verify(guestServiceClient, never()).getGuestById(any(), any());
    }

    @Test
    void rejectsNewGuestWithoutLookupIdentifierAndGuestNotFound() {
        ReservationGuestRequestDto noIdentifier = guest(null, true, "Ava", "Guest", null);
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(noIdentifier)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires a phone number");

        ReservationGuestRequestDto missingExisting = guest(99L, true, "Ava", "Guest", "5551000");
        when(guestServiceClient.getGuestById(99L, "PROP001")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(missingExisting)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Guest profile not found");
    }

    @Test
    void rejectsAmbiguousGuestLookupWithoutCreatingProfile() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenThrow(new BadRequestException(
                        "Guest lookup is ambiguous; select an existing guest profile explicitly"));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("select an existing guest profile explicitly");
        verify(guestServiceClient, never()).createGuest(any());
    }

    @Test
    void reusesExistingGuestWithoutUpdatingWhenEnrollmentIsFalse() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        GuestProfileResponse existingProfile = profile(42L, "PROP001");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(existingProfile));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
        verify(guestServiceClient, never()).createGuest(any());
    }

    @Test
    void alreadyEnrolledGuestKeepsMembershipAndIsNotEnrolledAgain() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse enrolledProfile = completeProfile(42L, "PROP001", "LOYALTY-42", "PLATINUM");
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(enrolledProfile));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true, "LOYALTY-42"));
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
        verify(guestServiceClient, never()).createGuest(any());
        verify(loyaltyServiceClient, never()).ensureMembership(any());
    }

    @Test
    void reusesExistingGuestWithValidLoyaltyWhenEnrollmentIsFalse() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        GuestProfileResponse enrolledProfile =
                completeProfile(42L, "PROP001", "LOYALTY-42", "STANDARD");
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(enrolledProfile));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true, "LOYALTY-42"));
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
    }

    @Test
    void rejectsInconsistentLoyaltyDataForBothEnrollmentChoices() {
        GuestProfileResponse membershipWithoutTier =
                completeProfile(42L, "PROP001", "LOYALTY-42", null);
        GuestProfileResponse tierWithoutMembership =
                completeProfile(43L, "PROP001", null, "STANDARD");

        for (GuestProfileResponse profile : List.of(membershipWithoutTier, tierWithoutMembership)) {
            when(guestServiceClient.getGuestById(profile.getId(), "PROP001")).thenReturn(Optional.of(profile));
            for (boolean enrollGuest : List.of(false, true)) {
                ReservationGuestRequestDto request =
                        guest(profile.getId(), true, "Ava", "Guest", "5551000");
                request.setEnrollGuest(enrollGuest);

                assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("inconsistent loyalty data");
            }
        }

        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
    }

    @Test
    void enrollmentFailureReportsGuestAndProgramItBelongsTo() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        request.setLoyaltyProgramId("44444444-4444-4444-4444-444444444444");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(enrollableProfile(42L, "PROP001", null, null));
        when(loyaltyServiceClient.ensureMembership(LOYALTY_GUEST_ID,
                        UUID.fromString("44444444-4444-4444-4444-444444444444")))
                .thenThrow(new com.pms.guestlisting.exception.ExternalServiceException(
                        "Failed to ensure loyalty membership: relation missing"));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(com.pms.guestlisting.exception.ExternalServiceException.class)
                .hasMessageContaining("guest profile 42")
                .hasMessageContaining("Ava Guest")
                .hasMessageContaining("44444444-4444-4444-4444-444444444444")
                .hasMessageContaining("relation missing");
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
    }

    @Test
    void rejectsEnrollmentWhenMembershipIsNotStoredOnTheGuestProfile() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(enrollableProfile(42L, "PROP001", null, null));
        when(loyaltyServiceClient.ensureMembership(LOYALTY_GUEST_ID))
                .thenReturn(membership("LOY00000042", "GOLD", "Gold", 2));
        // Guest service accepts the update but returns the profile without the loyalty values stored.
        when(guestServiceClient.updateGuestProfile(eq(42L), eq("PROP001"), any(GuestProfileUpdateRequest.class)))
                .thenReturn(enrollableProfile(42L, "PROP001", null, null));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(com.pms.guestlisting.exception.ExternalServiceException.class)
                .hasMessageContaining("LOY00000042")
                .hasMessageContaining("GOLD")
                .hasMessageContaining("loyaltyMembershipNumber and loyaltyTier")
                .hasMessageContaining("did not store");
    }

    @Test
    void rejectsEnrollmentWhenGuestIdIsNotUsableForLoyalty() {        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse existingProfile = completeProfile(42L, "PROP001", null, null).toBuilder()
                .guestId("NOT-A-UUID")
                .build();
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(existingProfile));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be enrolled in membership");
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
        verify(loyaltyServiceClient, never()).ensureMembership(any());
    }

    @Test
    void propertyScopedLookupReusesProfileWithoutUpdatingIt() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(profile(42L, "PROP002")));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP002", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
    }

    @Test
    void reusesExistingGuestIdWithoutUpdatingProfile() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(false);
        GuestProfileResponse existingProfile = profile(42L, "PROP001");
        when(guestServiceClient.getGuestById(42L, "PROP001")).thenReturn(Optional.of(existingProfile));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient, never()).updateGuestProfile(any(), any(), any());
    }

    private ReservationGuestRequestDto guest(
            Long guestProfileId,
            boolean primary,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        ReservationGuestRequestDto request = new ReservationGuestRequestDto();
        request.setGuestProfileId(guestProfileId);
        request.setIsPrimary(primary);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhoneNumber(phoneNumber);
        return request;
    }

    private GuestProfileResponse profile(Long id, String propertyId) {
        return GuestProfileResponse.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId(propertyId)
                .firstName("Ava")
                .lastName("Guest")
                .vipStatus(false)
                .build();
    }

    private GuestProfileResponse enrollableProfile(
            Long id,
            String propertyId,
            String loyaltyMembershipNumber,
            String loyaltyTier
    ) {
        return completeProfile(id, propertyId, loyaltyMembershipNumber, loyaltyTier);
    }

    private GuestProfileResponse completeProfile(
            Long id,
            String propertyId,
            String loyaltyMembershipNumber,
            String loyaltyTier
    ) {
        return GuestProfileResponse.builder()
                .id(id)
                .guestId("GST-" + LOYALTY_GUEST_ID)
                .propertyId(propertyId)
                .salutation("Ms")
                .firstName("Ava")
                .lastName("Guest")
                .personalEmail("ava@example.com")
                .officialEmail("ava@work.example")
                .phoneNumber("5551000")
                .mobileNumber("5552000")
                .address("1 Main Street")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .nationality("American")
                .dateOfBirth(LocalDate.of(1990, 1, 2))
                .gender("Female")
                .companyName("Example Inc.")
                .vipStatus(true)
                .idType("PASSPORT")
                .idNumber("P1234567")
                .idDocumentPath("uploads/id.png")
                .loyaltyMembershipNumber(loyaltyMembershipNumber)
                .loyaltyTier(loyaltyTier)
                .build();
    }

    private LoyaltyMembershipResponse membership(
            String loyaltyNumber,
            String tierCode,
            String tierName,
            Integer tierLevel
    ) {
        return LoyaltyMembershipResponse.builder()
                .membershipId(UUID.randomUUID())
                .guestId(LOYALTY_GUEST_ID)
                .loyaltyNumber(loyaltyNumber)
                .tierCode(tierCode)
                .tierName(tierName)
                .tierLevel(tierLevel)
                .status("ACTIVE")
                .newlyEnrolled(true)
                .build();
    }
}
