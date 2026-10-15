package com.pms.guest.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pms.guest.dto.response.GuestDetailsResponse;
import com.pms.guest.dto.response.GuestDetailsResponse.GuestAssignmentDetails;
import com.pms.guest.entity.GuestProfile;
import com.pms.guest.exception.ReservationServiceException;
import com.pms.guest.integration.ReservationServiceClient;
import com.pms.guest.integration.dto.ReservationGuestAssignment;
import com.pms.guest.repository.GuestProfileRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestDetailsServiceImplTest {

    private static final String PROPERTY = "PROP-A";

    @Mock
    private GuestProfileRepository guestProfileRepository;

    @Mock
    private ReservationServiceClient reservationServiceClient;

    private GuestDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuestDetailsServiceImpl(guestProfileRepository, reservationServiceClient);
    }

    // ---------- phone ----------

    @Test
    void phoneMatchesPhoneNumberAndReturnsBookingContext() {
        GuestProfile guest = profile(25L, "Ava", "+1-555-0101", null);
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "+1-555-0101")).thenReturn(List.of(guest));
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "+1-555-0101")).thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(25L)))
                .thenReturn(List.of(assignment(101L, "CONF-1", 25L, true)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, " +1-555-0101 ", null, null, null);

        assertThat(response.getPropertyId()).isEqualTo(PROPERTY);
        assertThat(response.getGuests()).singleElement().satisfies(entry -> {
            assertThat(entry.getBookingId()).isEqualTo(101L);
            assertThat(entry.getConfirmationNumber()).isEqualTo("CONF-1");
            assertThat(entry.getGuestProfileId()).isEqualTo(25L);
            assertThat(entry.getIsPrimary()).isTrue();
            assertThat(entry.getGuest().getGuestId()).isEqualTo("GST-25");
            assertThat(entry.getGuest().getPhoneNumber()).isEqualTo("+1-555-0101");
            assertThat(entry.getGuest().getIdDocumentPath()).isEqualTo("uploads/id-25.png");
            assertThat(entry.getGuest().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        });
    }

    @Test
    void phoneMatchesMobileNumber() {
        GuestProfile guest = profile(31L, "Ben", null, null);
        guest.setMobileNumber("+91-99999");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "+91-99999")).thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "+91-99999")).thenReturn(List.of(guest));
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(31L))).thenReturn(List.of());

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, "+91-99999", null, null, null);

        assertThat(response.getGuests()).singleElement().satisfies(entry -> {
            assertThat(entry.getGuestProfileId()).isEqualTo(31L);
            assertThat(entry.getGuest().getMobileNumber()).isEqualTo("+91-99999");
            // No reservation assignment: guest returned without booking context.
            assertThat(entry.getBookingId()).isNull();
            assertThat(entry.getConfirmationNumber()).isNull();
            assertThat(entry.getIsPrimary()).isNull();
        });
    }

    @Test
    void phoneReturnsAllMatchingGuestsWithoutSelectingOne() {
        GuestProfile a = profile(25L, "Ava", "555", null);
        GuestProfile b = profile(42L, "Cleo", null, null);
        b.setMobileNumber("555");
        GuestProfile aAgain = profile(25L, "Ava", "555", null);
        aAgain.setMobileNumber("555");
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "555")).thenReturn(List.of(a));
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "555")).thenReturn(List.of(b, aAgain));
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(25L, 42L)))
                .thenReturn(List.of(assignment(101L, "CONF-1", 25L, true), assignment(102L, "CONF-2", 42L, true)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, "555", null, null, null);

        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getGuestProfileId)
                .containsExactly(25L, 42L);
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getBookingId)
                .containsExactly(101L, 102L);
    }

    @Test
    void guestWithMultipleReservationAssignmentsReturnsEachAssignment() {
        GuestProfile guest = profile(25L, "Ava", "555", null);
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "555")).thenReturn(List.of(guest));
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "555")).thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(25L)))
                .thenReturn(List.of(
                        assignment(205L, "CONF-9", 25L, false),
                        assignment(101L, "CONF-1", 25L, true),
                        assignment(102L, "CONF-1", 25L, true)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, "555", null, null, null);

        assertThat(response.getGuests()).hasSize(3);
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getBookingId)
                .containsExactly(101L, 102L, 205L);
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getIsPrimary)
                .containsExactly(true, true, false);
        assertThat(response.getGuests()).allSatisfy(entry ->
                assertThat(entry.getGuest().getGuestId()).isEqualTo("GST-25"));
    }

    // ---------- email ----------

    @Test
    void emailMatchesPersonalEmail() {
        GuestProfile guest = profile(25L, "Ava", null, "ava@example.com");
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail(PROPERTY, "ava@example.com"))
                .thenReturn(List.of(guest));
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail(PROPERTY, "ava@example.com"))
                .thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(25L)))
                .thenReturn(List.of(assignment(101L, "CONF-1", 25L, true)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, "ava@example.com", null, null);

        assertThat(response.getGuests()).singleElement().satisfies(entry -> {
            assertThat(entry.getGuest().getPersonalEmail()).isEqualTo("ava@example.com");
            assertThat(entry.getBookingId()).isEqualTo(101L);
        });
    }

    @Test
    void emailMatchesOfficialEmail() {
        GuestProfile guest = profile(31L, "Ben", null, null);
        guest.setOfficialEmail("ben@corp.example");
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail(PROPERTY, "ben@corp.example"))
                .thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail(PROPERTY, "ben@corp.example"))
                .thenReturn(List.of(guest));
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(31L))).thenReturn(List.of());

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, "ben@corp.example", null, null);

        assertThat(response.getGuests()).singleElement()
                .satisfies(entry -> assertThat(entry.getGuest().getOfficialEmail()).isEqualTo("ben@corp.example"));
    }

    @Test
    void emailIsPassedToRepositoryWithoutCaseNormalization() {
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail(PROPERTY, "Ava@Example.com"))
                .thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail(PROPERTY, "Ava@Example.com"))
                .thenReturn(List.of());

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, "Ava@Example.com", null, null);

        assertThat(response.getGuests()).isEmpty();
        verify(guestProfileRepository, never()).findByPropertyIdAndPersonalEmail(PROPERTY, "ava@example.com");
        verifyNoInteractions(reservationServiceClient);
    }

    @Test
    void emailReturnsMultipleGuests() {
        GuestProfile a = profile(25L, "Ava", null, "shared@example.com");
        GuestProfile b = profile(42L, "Cleo", null, null);
        b.setOfficialEmail("shared@example.com");
        when(guestProfileRepository.findByPropertyIdAndPersonalEmail(PROPERTY, "shared@example.com"))
                .thenReturn(List.of(a));
        when(guestProfileRepository.findByPropertyIdAndOfficialEmail(PROPERTY, "shared@example.com"))
                .thenReturn(List.of(b));
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(25L, 42L)))
                .thenReturn(List.of());

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, "shared@example.com", null, null);

        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getGuestProfileId)
                .containsExactly(25L, 42L);
    }

    // ---------- bookingId ----------

    @Test
    void bookingIdReturnsPrimaryGuest() {
        when(reservationServiceClient.findAssignmentsByBookingId(PROPERTY, 101L))
                .thenReturn(List.of(assignment(101L, "CONF-1001", 25L, true)));
        when(guestProfileRepository.findByPropertyIdAndIdIn(eq(PROPERTY), eq(Set.of(25L))))
                .thenReturn(List.of(profile(25L, "Ava", "555", null)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, null, 101L, null);

        assertThat(response.getGuests()).singleElement().satisfies(entry -> {
            assertThat(entry.getIsPrimary()).isTrue();
            assertThat(entry.getConfirmationNumber()).isEqualTo("CONF-1001");
            assertThat(entry.getGuest().getFirstName()).isEqualTo("Ava");
        });
    }

    @Test
    void bookingIdReturnsAllGuestsAndExcludesOtherBookingsUnderSameConfirmation() {
        when(reservationServiceClient.findAssignmentsByBookingId(PROPERTY, 101L))
                .thenReturn(List.of(
                        assignment(101L, "CONF-1001", 25L, true),
                        assignment(101L, "CONF-1001", 31L, false),
                        // Defensive: a sibling booking under the same confirmation must never leak.
                        assignment(102L, "CONF-1001", 42L, false)));
        when(guestProfileRepository.findByPropertyIdAndIdIn(eq(PROPERTY), eq(Set.of(25L, 31L))))
                .thenReturn(List.of(profile(25L, "Ava", "555", null), profile(31L, "Ben", null, null)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, null, 101L, null);

        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getGuestProfileId)
                .containsExactly(25L, 31L);
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getBookingId)
                .containsOnly(101L);
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getIsPrimary)
                .containsExactly(true, false);
        verify(reservationServiceClient, never()).findAssignmentsByConfirmationNumber(anyString(), anyString());
    }

    // ---------- confirmationNumber ----------

    @Test
    void confirmationNumberReturnsAllBookingsAndPreservesRepeatedGuestPerBooking() {
        when(reservationServiceClient.findAssignmentsByConfirmationNumber(PROPERTY, "CONF-1001"))
                .thenReturn(List.of(
                        assignment(101L, "CONF-1001", 25L, true),
                        assignment(101L, "CONF-1001", 31L, false),
                        assignment(102L, "CONF-1001", 25L, true),
                        assignment(102L, "CONF-1001", 42L, false)));
        when(guestProfileRepository.findByPropertyIdAndIdIn(eq(PROPERTY), eq(Set.of(25L, 31L, 42L))))
                .thenReturn(List.of(
                        profile(25L, "Ava", "555", null),
                        profile(31L, "Ben", null, null),
                        profile(42L, "Cleo", null, null)));

        GuestDetailsResponse response = service.getGuestDetails(PROPERTY, null, null, null, "CONF-1001");

        assertThat(response.getGuests()).hasSize(4);
        assertThat(response.getGuests())
                .extracting(GuestAssignmentDetails::getBookingId, GuestAssignmentDetails::getGuestProfileId,
                        GuestAssignmentDetails::getIsPrimary)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, 25L, true),
                        org.assertj.core.groups.Tuple.tuple(101L, 31L, false),
                        org.assertj.core.groups.Tuple.tuple(102L, 25L, true),
                        org.assertj.core.groups.Tuple.tuple(102L, 42L, false));
        assertThat(response.getGuests()).extracting(GuestAssignmentDetails::getBookingId)
                .containsOnly(101L, 102L);
        assertThat(response.getGuests()).allSatisfy(entry -> assertThat(entry.getGuest()).isNotNull());
    }

    // ---------- membership ----------

    @Test
    void membershipEnrolledWhenBothLoyaltyFieldsPresent() {
        GuestProfile guest = profile(25L, "Ava", "555", null);
        guest.setLoyaltyMembershipNumber("TEMP-GUEST");
        guest.setLoyaltyTier("STANDARD");
        stubPhone(guest);

        GuestAssignmentDetails entry = service.getGuestDetails(PROPERTY, "555", null, null, null)
                .getGuests().get(0);

        assertThat(entry.getMembership().isEnrolled()).isTrue();
        assertThat(entry.getMembership().getMembershipNumber()).isEqualTo("TEMP-GUEST");
        assertThat(entry.getMembership().getTier()).isEqualTo("STANDARD");
    }

    @Test
    void membershipNotEnrolledWhenLoyaltyFieldsNullOrBlank() {
        GuestProfile guest = profile(25L, "Ava", "555", null);
        guest.setLoyaltyMembershipNumber(" ");
        guest.setLoyaltyTier(null);
        stubPhone(guest);

        GuestAssignmentDetails entry = service.getGuestDetails(PROPERTY, "555", null, null, null)
                .getGuests().get(0);

        assertThat(entry.getMembership().isEnrolled()).isFalse();
        assertThat(entry.getMembership().getMembershipNumber()).isNull();
        assertThat(entry.getMembership().getTier()).isNull();
    }

    // ---------- validation / empty ----------

    @Test
    void propertyIdIsMandatory() {
        assertThatThrownBy(() -> service.getGuestDetails(" ", "555", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("propertyId is required");
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void noCriterionIsRejected() {
        assertThatThrownBy(() -> service.getGuestDetails(PROPERTY, " ", null, null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one");
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void multipleCriteriaAreRejected() {
        assertThatThrownBy(() -> service.getGuestDetails(PROPERTY, "555", null, 101L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one");
        verifyNoInteractions(guestProfileRepository, reservationServiceClient);
    }

    @Test
    void noMatchesReturnEmptyResultForEveryMode() {
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "000")).thenReturn(List.of());
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "000")).thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByBookingId(PROPERTY, 999L)).thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByConfirmationNumber(PROPERTY, "NONE")).thenReturn(List.of());

        assertThat(service.getGuestDetails(PROPERTY, "000", null, null, null).getGuests()).isEmpty();
        assertThat(service.getGuestDetails(PROPERTY, null, null, 999L, null).getGuests()).isEmpty();
        assertThat(service.getGuestDetails(PROPERTY, null, null, null, "NONE").getGuests()).isEmpty();
        verify(guestProfileRepository, never()).findByPropertyIdAndIdIn(anyString(), anyCollection());
    }

    @Test
    void reservationServiceFailurePropagates() {
        when(reservationServiceClient.findAssignmentsByBookingId(PROPERTY, 101L))
                .thenThrow(new ReservationServiceException("down"));

        assertThatThrownBy(() -> service.getGuestDetails(PROPERTY, null, null, 101L, null))
                .isInstanceOf(ReservationServiceException.class);
        verify(guestProfileRepository, never()).findByPropertyIdAndIdIn(any(), any());
    }

    private void stubPhone(GuestProfile guest) {
        when(guestProfileRepository.findByPropertyIdAndPhoneNumber(PROPERTY, "555")).thenReturn(List.of(guest));
        when(guestProfileRepository.findByPropertyIdAndMobileNumber(PROPERTY, "555")).thenReturn(List.of());
        when(reservationServiceClient.findAssignmentsByGuestProfileIds(PROPERTY, List.of(guest.getId())))
                .thenReturn(List.of(assignment(101L, "CONF-1", guest.getId(), true)));
    }

    private static ReservationGuestAssignment assignment(
            Long bookingId, String confirmation, Long guestProfileId, boolean primary) {
        return ReservationGuestAssignment.builder()
                .bookingId(bookingId)
                .confirmationNumber(confirmation)
                .guestProfileId(guestProfileId)
                .isPrimary(primary)
                .build();
    }

    private static GuestProfile profile(Long id, String firstName, String phone, String personalEmail) {
        return GuestProfile.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId(PROPERTY)
                .firstName(firstName)
                .lastName("Guest")
                .phoneNumber(phone)
                .personalEmail(personalEmail)
                .dateOfBirth(LocalDate.of(1990, 1, 2))
                .vipStatus(false)
                .idDocumentPath("uploads/id-" + id + ".png")
                .build();
    }
}
