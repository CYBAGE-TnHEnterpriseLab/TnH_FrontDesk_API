package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.entity.ReservationGuest;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationGuestRepository;
import com.pms.reservation.service.ResolvedReservationGuest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class ReservationGuestServiceImplTest {

    @Mock
    private ReservationGuestRepository reservationGuestRepository;

    @Mock
    private ReservationBookingRepository reservationBookingRepository;

    @Mock
    private GuestServiceClient guestServiceClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private ReservationGuestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationGuestServiceImpl(
                reservationGuestRepository,
                reservationBookingRepository,
                guestServiceClient,
                transactionManager
        );
    }

    @Test
    void assigningPrimaryClearsCurrentPrimaryBeforeSavingNewRelationship() {
        ReservationGuest currentPrimary = ReservationGuest.builder()
                .id(10L)
                .bookingId(1L)
                .guestProfileId(100L)
                .isPrimary(true)
                .build();
        GuestProfileResponse profile = guestProfile(200L);

        when(reservationBookingRepository.findById(1L)).thenReturn(Optional.of(booking("property-1")));
        when(guestServiceClient.getGuestById(200L, "property-1")).thenReturn(Optional.of(profile));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationGuestRepository.findByBookingIdAndGuestProfileId(1L, 200L))
                .thenReturn(Optional.empty());
        when(reservationGuestRepository.findByBookingIdAndIsPrimaryTrue(1L))
                .thenReturn(Optional.of(currentPrimary));
        when(reservationGuestRepository.saveAndFlush(any(ReservationGuest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.assignGuestToBooking(1L, 200L, true);

        assertThat(currentPrimary.getIsPrimary()).isFalse();
        assertThat(response.getGuestProfileId()).isEqualTo(200L);
        assertThat(response.getIsPrimary()).isTrue();
        verify(reservationGuestRepository).saveAndFlush(currentPrimary);
    }

    @Test
    void assignsResolvedGuestsToBookingIdWithoutCallingGuestService() {
        List<ResolvedReservationGuest> guests = List.of(
                new ResolvedReservationGuest(125L, true),
                new ResolvedReservationGuest(126L, false),
                new ResolvedReservationGuest(127L, false));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationBookingRepository.existsById(101L)).thenReturn(true);

        service.assignResolvedGuests(101L, guests);

        org.mockito.ArgumentCaptor<List<ReservationGuest>> relationships =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(reservationGuestRepository).saveAllAndFlush(relationships.capture());
        assertThat(relationships.getValue())
                .extracting(ReservationGuest::getBookingId)
                .containsOnly(101L);
        assertThat(relationships.getValue())
                .extracting(ReservationGuest::getGuestProfileId)
                .containsExactly(125L, 126L, 127L);
        assertThat(relationships.getValue())
                .extracting(ReservationGuest::getIsPrimary)
                .containsExactly(true, false, false);
        org.mockito.Mockito.verifyNoInteractions(guestServiceClient);
    }

    @Test
    void assigningExistingRelationshipIsIdempotent() {
        ReservationGuest existing = ReservationGuest.builder()
                .id(10L)
                .bookingId(1L)
                .guestProfileId(200L)
                .isPrimary(false)
                .build();
        GuestProfileResponse profile = guestProfile(200L);

        when(reservationBookingRepository.findById(1L)).thenReturn(Optional.of(booking("property-1")));
        when(guestServiceClient.getGuestById(200L, "property-1")).thenReturn(Optional.of(profile));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationGuestRepository.findByBookingIdAndGuestProfileId(1L, 200L))
                .thenReturn(Optional.of(existing));

        var response = service.assignGuestToBooking(1L, 200L, false);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getIsPrimary()).isFalse();
        verify(reservationGuestRepository, org.mockito.Mockito.never())
                .saveAndFlush(any(ReservationGuest.class));
    }

    @Test
    void removingPrimaryGuestIsRejected() {
        ReservationGuest primary = ReservationGuest.builder()
                .id(10L)
                .bookingId(1L)
                .guestProfileId(200L)
                .isPrimary(true)
                .build();

        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationBookingRepository.existsById(1L)).thenReturn(true);
        when(reservationGuestRepository.findByBookingIdAndGuestProfileId(1L, 200L))
                .thenReturn(Optional.of(primary));

        assertThatThrownBy(() -> service.removeGuestFromBooking(1L, 200L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("until another guest is assigned as primary");
    }

    @Test
    void makePrimaryClearsExistingPrimaryBeforeFlushingSelectedGuest() {
        ReservationGuest currentPrimary = ReservationGuest.builder()
                .id(10L)
                .bookingId(1L)
                .guestProfileId(100L)
                .isPrimary(true)
                .build();
        ReservationGuest selected = ReservationGuest.builder()
                .id(20L)
                .bookingId(1L)
                .guestProfileId(200L)
                .isPrimary(false)
                .build();
        GuestProfileResponse profile = guestProfile(200L);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationBookingRepository.existsById(1L)).thenReturn(true);
        when(reservationGuestRepository.findByIdAndBookingId(20L, 1L)).thenReturn(Optional.of(selected));
        when(reservationGuestRepository.findByBookingIdAndIsPrimaryTrue(1L))
                .thenReturn(Optional.of(currentPrimary));
        when(reservationGuestRepository.saveAndFlush(any(ReservationGuest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationBookingRepository.findById(1L)).thenReturn(Optional.of(booking("property-1")));
        when(guestServiceClient.getGuestById(200L, "property-1")).thenReturn(Optional.of(profile));

        var response = service.makePrimaryGuest(1L, 20L);

        assertThat(currentPrimary.getIsPrimary()).isFalse();
        assertThat(selected.getIsPrimary()).isTrue();
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getGuestProfile()).isSameAs(profile);
        InOrder saves = inOrder(reservationGuestRepository);
        saves.verify(reservationGuestRepository).saveAndFlush(currentPrimary);
        saves.verify(reservationGuestRepository).saveAndFlush(selected);
    }

    @Test
    void makePrimaryRejectsRelationshipFromAnotherBooking() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(reservationBookingRepository.existsById(1L)).thenReturn(true);
        when(reservationGuestRepository.findByIdAndBookingId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.makePrimaryGuest(1L, 20L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to the specified booking");
    }

    private GuestProfileResponse guestProfile(Long id) {
        return GuestProfileResponse.builder()
                .id(id)
                .guestId("GST-" + id)
                .propertyId("property-1")
                .build();
    }

    private ReservationBookingRecord booking(String propertyId) {
        return ReservationBookingRecord.builder().propertyId(propertyId).build();
    }

    @Test
    void assignmentsByBookingIdAreScopedToThatBookingOnly() {
        when(reservationBookingRepository.findByIdAndPropertyId(101L, "P1"))
                .thenReturn(Optional.of(bookingRecord(101L, "CONF-1")));
        when(reservationGuestRepository.findByBookingIdInOrderByBookingIdAscIdAsc(List.of(101L)))
                .thenReturn(List.of(relationship(101L, 25L, true), relationship(101L, 31L, false)));

        var result = service.findGuestAssignments("P1", 101L, null, null);

        assertThat(result).extracting("bookingId", "confirmationNumber", "guestProfileId", "isPrimary")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, "CONF-1", 25L, true),
                        org.assertj.core.groups.Tuple.tuple(101L, "CONF-1", 31L, false));
    }

    @Test
    void assignmentsByBookingIdInAnotherPropertyAreEmpty() {
        when(reservationBookingRepository.findByIdAndPropertyId(101L, "P2")).thenReturn(Optional.empty());

        assertThat(service.findGuestAssignments("P2", 101L, null, null)).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(reservationGuestRepository);
    }

    @Test
    void assignmentsByConfirmationReturnEveryBookingAndRepeatedGuest() {
        when(reservationBookingRepository.findByPropertyIdAndConfirmationNumberOrderByIdAsc("P1", "CONF-1"))
                .thenReturn(List.of(bookingRecord(101L, "CONF-1"), bookingRecord(102L, "CONF-1")));
        when(reservationGuestRepository.findByBookingIdInOrderByBookingIdAscIdAsc(List.of(101L, 102L)))
                .thenReturn(List.of(
                        relationship(101L, 25L, true),
                        relationship(101L, 31L, false),
                        relationship(102L, 25L, true),
                        relationship(102L, 42L, false)));

        var result = service.findGuestAssignments("P1", null, " CONF-1 ", null);

        assertThat(result).extracting("bookingId", "guestProfileId")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, 25L),
                        org.assertj.core.groups.Tuple.tuple(101L, 31L),
                        org.assertj.core.groups.Tuple.tuple(102L, 25L),
                        org.assertj.core.groups.Tuple.tuple(102L, 42L));
    }

    @Test
    void assignmentsByGuestProfileIdsDropBookingsFromOtherProperties() {
        when(reservationGuestRepository.findByGuestProfileIdInOrderByBookingIdAscIdAsc(
                new java.util.LinkedHashSet<>(List.of(25L))))
                .thenReturn(List.of(relationship(101L, 25L, true), relationship(900L, 25L, true)));
        when(reservationBookingRepository.findByPropertyIdAndIdIn(
                "P1", new java.util.LinkedHashSet<>(List.of(101L, 900L))))
                .thenReturn(List.of(bookingRecord(101L, "CONF-1")));

        var result = service.findGuestAssignments("P1", null, null, List.of(25L));

        assertThat(result).singleElement().satisfies(a -> {
            assertThat(a.getBookingId()).isEqualTo(101L);
            assertThat(a.getConfirmationNumber()).isEqualTo("CONF-1");
        });
    }

    @Test
    void assignmentsRequireExactlyOneCriterionAndProperty() {
        assertThatThrownBy(() -> service.findGuestAssignments(" ", 1L, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.findGuestAssignments("P1", null, null, List.of()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.findGuestAssignments("P1", 1L, "CONF-1", null))
                .isInstanceOf(BadRequestException.class);
    }

    private ReservationBookingRecord bookingRecord(Long id, String confirmationNumber) {
        return ReservationBookingRecord.builder()
                .id(id)
                .propertyId("P1")
                .confirmationNumber(confirmationNumber)
                .build();
    }

    private ReservationGuest relationship(Long bookingId, Long guestProfileId, boolean primary) {
        return ReservationGuest.builder()
                .bookingId(bookingId)
                .guestProfileId(guestProfileId)
                .isPrimary(primary)
                .build();
    }
}
