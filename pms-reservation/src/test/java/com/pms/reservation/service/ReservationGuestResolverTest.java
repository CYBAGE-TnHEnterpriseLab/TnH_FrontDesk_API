package com.pms.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationGuestRequestDto;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileCreateRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.integration.dto.GuestProfileUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationGuestResolverTest {

    @Mock
    private GuestServiceClient guestServiceClient;

    private ReservationGuestResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ReservationGuestResolver(guestServiceClient);
    }

    @Test
    void resolvesExistingGuestByIdAndRejectsDifferentProperty() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        when(guestServiceClient.getGuestById(42L)).thenReturn(Optional.of(profile(42L, "PROP001")));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient, never()).findExistingGuest(any());
        verify(guestServiceClient, never()).createGuest(any());

        when(guestServiceClient.getGuestById(42L)).thenReturn(Optional.of(profile(42L, "PROP002")));
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
    void createsNewGuestWithTemporaryLoyaltyValuesWhenEnrolled() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        request.setDateOfBirth(LocalDate.of(1990, 1, 2));
        request.setIdDocumentPath("uploads/id.png");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class))).thenReturn(Optional.empty());
        when(guestServiceClient.createGuest(any(GuestProfileCreateRequest.class)))
                .thenReturn(profile(42L, "PROP001"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        ArgumentCaptor<GuestProfileCreateRequest> createRequest =
                ArgumentCaptor.forClass(GuestProfileCreateRequest.class);
        verify(guestServiceClient).createGuest(createRequest.capture());
        assertThat(createRequest.getValue().getPropertyId()).isEqualTo("PROP001");
        assertThat(createRequest.getValue().getFirstName()).isEqualTo("Ava");
        assertThat(createRequest.getValue().getVipStatus()).isFalse();
        assertThat(createRequest.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(createRequest.getValue().getIdDocumentPath()).isEqualTo("uploads/id.png");
        assertThat(createRequest.getValue().getLoyaltyMembershipNumber()).isEqualTo("TEMP-GUEST");
        assertThat(createRequest.getValue().getLoyaltyTier()).isEqualTo("STANDARD");
        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
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

        verify(guestServiceClient, never()).getGuestById(any());
    }

    @Test
    void rejectsNewGuestWithoutLookupIdentifierAndGuestNotFound() {
        ReservationGuestRequestDto noIdentifier = guest(null, true, "Ava", "Guest", null);
        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(noIdentifier)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires a phone number");

        ReservationGuestRequestDto missingExisting = guest(99L, true, "Ava", "Guest", "5551000");
        when(guestServiceClient.getGuestById(99L)).thenReturn(Optional.empty());
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
    void enrollsLookupMatchWhenGuestHasNoMembership() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse existingProfile = profile(42L, "PROP001");
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(existingProfile));
        when(guestServiceClient.updateGuestProfile(any(), any(GuestProfileUpdateRequest.class)))
                .thenReturn(enrolledProfile(42L, "PROP001"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        ArgumentCaptor<GuestProfileUpdateRequest> updateRequest =
                ArgumentCaptor.forClass(GuestProfileUpdateRequest.class);
        verify(guestServiceClient).updateGuestProfile(org.mockito.ArgumentMatchers.eq(42L), updateRequest.capture());
        assertThat(updateRequest.getValue().getFirstName()).isEqualTo("Ava");
        assertThat(updateRequest.getValue().getLoyaltyMembershipNumber()).isEqualTo("TEMP-GUEST");
        assertThat(updateRequest.getValue().getLoyaltyTier()).isEqualTo("STANDARD");
        verify(guestServiceClient, never()).createGuest(any());
    }

    @Test
    void rejectsEnrollmentWhenExistingGuestAlreadyHasMembership() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse enrolledProfile = GuestProfileResponse.builder()
                .id(42L)
                .guestId("GST-42")
                .propertyId("PROP001")
                .loyaltyMembershipNumber("LOYALTY-42")
                .loyaltyTier("STANDARD")
                .build();
        when(guestServiceClient.getGuestById(42L)).thenReturn(Optional.of(enrolledProfile));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has a loyalty membership");
        verify(guestServiceClient, never()).createGuest(any());
    }

    @Test
    void rejectsExistingGuestWithOnlyLoyaltyMembershipNumberOrTier() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        GuestProfileResponse partialMembership = GuestProfileResponse.builder()
                .id(42L)
                .guestId("GST-42")
                .propertyId("PROP001")
                .loyaltyTier("STANDARD")
                .build();
        when(guestServiceClient.getGuestById(42L)).thenReturn(Optional.of(partialMembership));

        assertThatThrownBy(() -> resolver.resolveGuests("PROP001", List.of(request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must both be set or both be null");
    }

    @Test
    void propertyScopedLookupAllowsEnrollmentWhenThatPropertyProfileHasNoMembership() {
        ReservationGuestRequestDto request = guest(null, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        when(guestServiceClient.findExistingGuest(any(GuestLookupRequest.class)))
                .thenReturn(Optional.of(profile(42L, "PROP002")));
        when(guestServiceClient.updateGuestProfile(any(), any(GuestProfileUpdateRequest.class)))
                .thenReturn(enrolledProfile(42L, "PROP002"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP002", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient).updateGuestProfile(
                org.mockito.ArgumentMatchers.eq(42L),
                any(GuestProfileUpdateRequest.class));
    }

    @Test
    void enrollsExistingGuestIdWhenNoMembershipIsAssigned() {
        ReservationGuestRequestDto request = guest(42L, true, "Ava", "Guest", "5551000");
        request.setEnrollGuest(true);
        GuestProfileResponse existingProfile = profile(42L, "PROP001");
        when(guestServiceClient.getGuestById(42L)).thenReturn(Optional.of(existingProfile));
        when(guestServiceClient.updateGuestProfile(any(), any(GuestProfileUpdateRequest.class)))
                .thenReturn(enrolledProfile(42L, "PROP001"));

        List<ResolvedReservationGuest> result = resolver.resolveGuests("PROP001", List.of(request));

        assertThat(result).containsExactly(new ResolvedReservationGuest(42L, true));
        verify(guestServiceClient).updateGuestProfile(
                org.mockito.ArgumentMatchers.eq(42L),
                any(GuestProfileUpdateRequest.class));
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

    private GuestProfileResponse enrolledProfile(Long id, String propertyId) {
        return GuestProfileResponse.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId(propertyId)
                .firstName("Ava")
                .lastName("Guest")
                .vipStatus(false)
                .loyaltyMembershipNumber("TEMP-GUEST")
                .loyaltyTier("STANDARD")
                .build();
    }
}
