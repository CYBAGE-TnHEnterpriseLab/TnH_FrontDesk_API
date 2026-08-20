package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationCheckoutServiceImplTest {

    @Mock
    private ReservationBookingRepository reservationBookingRepository;

    @Mock
    private ReservationCheckInAuditRepository auditRepository;

    @Mock
    private HousekeepingRoomStatusService housekeepingRoomStatusService;

    @InjectMocks
    private ReservationCheckoutServiceImpl service;

    private ReservationBookingRecord booking;
    private CheckoutRequestDto request;

    @BeforeEach
    void setUp() {
        booking = ReservationBookingRecord.builder()
                .id(11L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .assignedRoomNo("101")
                .departureDate(LocalDate.of(2026, 8, 11))
                .reservationStatus("CHECKED_IN")
                .build();
        request = new CheckoutRequestDto();
        request.setActor("front-desk-user");
        request.setBusinessDate(LocalDate.of(2026, 8, 11));
    }

    @Test
    void completeCheckoutShouldUpdateStatusTimestampAndRoomOccupancy() {
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        CheckoutCompletionResponseDto response = service.completeCheckout("CONF-101", request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_OUT");
        assertThat(response.getCheckOutCompletedAt()).isNotNull();
        assertThat(response.getCheckOutCompletedBy()).isEqualTo("front-desk-user");
        assertThat(booking.getCheckOutBusinessDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        verify(reservationBookingRepository).save(booking);
        verify(housekeepingRoomStatusService).markDirty(any());
        verify(auditRepository).save(any());
    }

    @Test
    void completeCheckoutShouldRejectReservationThatIsNotCheckedIn() {
        booking.setReservationStatus("CONFIRMED");
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.completeCheckout("CONF-101", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-out can only be initiated for a checked-in reservation");

        verify(reservationBookingRepository, never()).save(any());
        verify(housekeepingRoomStatusService, never()).markDirty(any());
    }

    @Test
    void completeCheckoutShouldRequireDepartureBusinessDate() {
        request.setBusinessDate(LocalDate.of(2026, 8, 10));
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.completeCheckout("CONF-101", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-out businessDate must match the reservation departureDate");

        verify(reservationBookingRepository, never()).save(any());
    }

    @Test
    void cancelCheckoutShouldRecheckInAndRestoreOccupancyOnSameBusinessDate() {
        booking.setReservationStatus("CHECKED_OUT");
        booking.setCheckOutBusinessDate(LocalDate.of(2026, 8, 11));
        booking.setCheckOutCompletedAt(java.time.LocalDateTime.now());
        booking.setCheckOutCompletedBy("front-desk-user");
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        CheckoutCompletionResponseDto response = service.cancelCheckout("CONF-101", request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        assertThat(response.getCheckOutCompletedAt()).isNull();
        assertThat(booking.getCheckOutBusinessDate()).isNull();
        verify(housekeepingRoomStatusService).markOccupied(any());
        verify(auditRepository).save(any());
    }

    @Test
    void cancelCheckoutShouldOnlyAllowSameBusinessDate() {
        booking.setReservationStatus("CHECKED_OUT");
        booking.setCheckOutBusinessDate(LocalDate.of(2026, 8, 10));
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelCheckout("CONF-101", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-out can only be cancelled on the same business date it was completed");

        verify(housekeepingRoomStatusService, never()).markOccupied(any());
    }
}
