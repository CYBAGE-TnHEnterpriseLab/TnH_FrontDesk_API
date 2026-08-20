package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.housekeeping.dto.HousekeepingRoomStatusResponseDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationCheckInWorkflowServiceImplTest {

    @Mock private ReservationBookingRepository reservationBookingRepository;
    @Mock private ReservationCheckInAuditRepository auditRepository;
    @Mock private HousekeepingRoomStatusService housekeepingRoomStatusService;
    @InjectMocks private ReservationCheckInWorkflowServiceImpl service;

    private ReservationBookingRecord booking;

    @BeforeEach
    void setUp() {
        booking = ReservationBookingRecord.builder()
                .id(10L).propertyId("PROP001").confirmationNumber("CONF-101")
                .reservationStatus("CONFIRMED").build();
    }

    @Test
    void completeCheckInShouldUseConfirmationNumberWithoutPaymentOrSignaturePrerequisites() {
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));
        when(housekeepingRoomStatusService.markOccupied(any()))
                .thenReturn(HousekeepingRoomStatusResponseDto.builder().build());
        CheckInCompleteRequestDto request = request();

        var response = service.completeCheckIn("CONF-101", request);

        assertThat(response.getConfirmationNumber()).isEqualTo("CONF-101");
        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        assertThat(booking.getCheckInCompletedBy()).isEqualTo("frontdesk.user");
        verify(reservationBookingRepository).findByConfirmationNumber("CONF-101");
        verify(reservationBookingRepository).save(booking);
        verify(housekeepingRoomStatusService).markOccupied(any());
        verify(auditRepository).save(any());
    }

    @Test
    void completeCheckInShouldBeIdempotentWhenAlreadyCheckedIn() {
        booking.setReservationStatus("CHECKED_IN");
        booking.setCheckInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));

        var response = service.completeCheckIn("CONF-101", request());

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        verify(housekeepingRoomStatusService, never()).markOccupied(any());
        verify(reservationBookingRepository, never()).save(any());
    }

    @Test
    void completeCheckInShouldUpgradeArrivedStatusWhenRequested() {
        booking.setReservationStatus("ARRIVED");
        booking.setCheckInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        when(reservationBookingRepository.findByConfirmationNumber("CONF-101")).thenReturn(Optional.of(booking));
        CheckInCompleteRequestDto request = request();
        request.setTargetStatus("CHECKED_IN");

        var response = service.completeCheckIn("CONF-101", request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        verify(housekeepingRoomStatusService, never()).markOccupied(any());
    }

    private CheckInCompleteRequestDto request() {
        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));
        return request;
    }
}
