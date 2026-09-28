package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.entity.ReservationGuest;
import com.pms.reservation.integration.GuestServiceClient;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationGuestRepository;
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

        when(reservationBookingRepository.existsById(1L)).thenReturn(true);
        when(guestServiceClient.getGuestById(200L)).thenReturn(Optional.of(profile));
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
    void assigningExistingRelationshipIsIdempotent() {
        ReservationGuest existing = ReservationGuest.builder()
                .id(10L)
                .bookingId(1L)
                .guestProfileId(200L)
                .isPrimary(false)
                .build();
        GuestProfileResponse profile = guestProfile(200L);

        when(reservationBookingRepository.existsById(1L)).thenReturn(true);
        when(guestServiceClient.getGuestById(200L)).thenReturn(Optional.of(profile));
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
        when(guestServiceClient.getGuestById(200L)).thenReturn(Optional.of(profile));

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
}
