package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.dto.HousekeepingRoomStatusResponseDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInGuestUpdateRequestDto;
import com.pms.reservation.dto.CheckInPaymentValidationResponseDto;
import com.pms.reservation.dto.CheckInRoomStayUpdateRequestDto;
import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInStepProgressResponseDto;
import com.pms.reservation.dto.CheckInWorkflowResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.entity.ReservationCheckInSignatureRecord;
import com.pms.reservation.entity.ReservationCheckInWorkflowRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.repository.ReservationCheckInSignatureRepository;
import com.pms.reservation.repository.ReservationCheckInWorkflowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationCheckInWorkflowServiceImplTest {

    @Mock
    private ReservationBookingRepository reservationBookingRepository;

    @Mock
    private ReservationCheckInWorkflowRepository workflowRepository;

    @Mock
    private ReservationCheckInSignatureRepository signatureRepository;

    @Mock
    private ReservationCheckInAuditRepository auditRepository;

    @Mock
    private HousekeepingRoomStatusService housekeepingRoomStatusService;

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @InjectMocks
    private ReservationCheckInWorkflowServiceImpl service;

    private ReservationBookingRecord booking;

    @BeforeEach
    void setUp() {
        booking = ReservationBookingRecord.builder()
                .id(10L)
                .propertyId("PROP001")
                .confirmationNumber("CONF-101")
                .reservationStatus("CONFIRMED")
                .personalEmail("guest@personal.com")
                .officialEmail("guest@official.com")
                .phoneNumber("+911111111111")
                .mobileNumber("+911111111111")
                .city("Pune")
                .country("India")
                .zipCode("411001")
                .roomType("Deluxe King")
                .numberOfRooms(1)
                .guestBalance(BigDecimal.ZERO)
                .arrivalDate(LocalDate.of(2026, 7, 22))
                .departureDate(LocalDate.of(2026, 7, 24))
                .build();
    }

    @Test
    void getWorkflowShouldCreateDefaultStateWhenAbsent() {
        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        CheckInWorkflowResponseDto response = service.getWorkflow(10L);

        assertThat(response.getCurrentStep()).isEqualTo("GUEST_DETAILS");
        assertThat(response.getProgressPercent()).isEqualTo(0);
    }

    @Test
    void updateGuestDetailsShouldCompleteStepAndMoveToRoomStay() {
        ReservationCheckInWorkflowRecord workflow = workflow("GUEST_DETAILS");

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        CheckInGuestUpdateRequestDto request = new CheckInGuestUpdateRequestDto();
        request.setPersonalEmail("new.personal@example.com");
        request.setOfficialEmail("new.official@example.com");
        request.setPhoneNumber("+91-8888888888");
        request.setMobileNumber("+91-9999999999");
        request.setCity("Mumbai");
        request.setCountry("India");
        request.setZipCode("400001");

        CheckInWorkflowResponseDto response = service.updateGuestDetails(10L, request, "agent1");

        assertThat(response.getCurrentStep()).isEqualTo("ROOM_STAY");
        assertThat(response.getCompletedSteps()).isEqualTo(1);
        verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
    }

    @Test
    void updateRoomStayShouldRejectWhenStepOutOfOrder() {
        ReservationCheckInWorkflowRecord workflow = workflow("GUEST_DETAILS");
        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));

        CheckInRoomStayUpdateRequestDto request = new CheckInRoomStayUpdateRequestDto();
        request.setRoomType("Suite");
        request.setRoomNo("501");

        assertThatThrownBy(() -> service.updateRoomStay(10L, request, "agent1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot execute step ROOM_STAY");

        verify(reservationBookingRepository, never()).save(any());
    }

    @Test
    void validatePaymentShouldBlockWhenOutstandingBalancePresent() {
        booking.setGuestBalance(new BigDecimal("1200.00"));
        ReservationCheckInWorkflowRecord workflow = workflow("PAYMENT_VALIDATION");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());
        workflow.setRoomStayCompletedAt(LocalDateTime.now());
        workflow.setSignatureCompletedAt(LocalDateTime.now());

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.validatePayment(10L, "agent1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Check-in blocked: outstanding guest balance must be settled before check-in");
    }

    @Test
    void completeCheckInShouldUpdateStatusAndOccupancy() {
        ReservationCheckInWorkflowRecord workflow = workflow("COMPLETE_CHECKIN");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());
        workflow.setRoomStayCompletedAt(LocalDateTime.now());
        workflow.setSignatureCompletedAt(LocalDateTime.now());
        workflow.setPaymentValidatedAt(LocalDateTime.now());

        ReservationCheckInSignatureRecord signature = ReservationCheckInSignatureRecord.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .contentType("image/png")
                .payloadBase64("abc")
                .signedAt(LocalDateTime.now())
                .build();

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.of(signature));
        when(housekeepingRoomStatusService.markOccupied(any())).thenReturn(HousekeepingRoomStatusResponseDto.builder().build());

        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));

        var response = service.completeCheckIn(10L, request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        verify(housekeepingRoomStatusService).markOccupied(any());
    }

    @Test
    void completeCheckInShouldSupportArrivedStatus() {
        ReservationCheckInWorkflowRecord workflow = workflow("COMPLETE_CHECKIN");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());
        workflow.setRoomStayCompletedAt(LocalDateTime.now());
        workflow.setSignatureCompletedAt(LocalDateTime.now());
        workflow.setPaymentValidatedAt(LocalDateTime.now());

        ReservationCheckInSignatureRecord signature = ReservationCheckInSignatureRecord.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .contentType("image/png")
                .payloadBase64("abc")
                .signedAt(LocalDateTime.now())
                .build();

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.of(signature));
        when(housekeepingRoomStatusService.markOccupied(any())).thenReturn(HousekeepingRoomStatusResponseDto.builder().build());

        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));
        request.setTargetStatus("ARRIVED");

        var response = service.completeCheckIn(10L, request);

        assertThat(response.getReservationStatus()).isEqualTo("ARRIVED");
        verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
    }

    @Test
    void updateRoomStayShouldValidateRoomTypeWhenPropertyWizardEnabled() {
        ReservationCheckInWorkflowRecord workflow = workflow("ROOM_STAY");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);

        PropertyInventoryValidationResponse validation = new PropertyInventoryValidationResponse();
        validation.setPropertyExists(true);
        validation.setRoomTypeAvailable(true);
        validation.setAvailableRooms(5);
        when(propertyInventoryPort.validateInventory(eq("PROP001"), eq("Suite"), eq(1))).thenReturn(validation);

        CheckInRoomStayUpdateRequestDto request = new CheckInRoomStayUpdateRequestDto();
        request.setRoomType("Suite");
        request.setRoomNo("701");

        CheckInWorkflowResponseDto response = service.updateRoomStay(10L, request, "agent1");

        assertThat(response.getCurrentStep()).isEqualTo("SIGNATURE");
        verify(propertyInventoryPort).validateInventory("PROP001", "Suite", 1);
    }

    @Test
    void validatePaymentShouldPassAndAdvanceToCompletionStep() {
        ReservationCheckInWorkflowRecord workflow = workflow("PAYMENT_VALIDATION");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());
        workflow.setRoomStayCompletedAt(LocalDateTime.now());
        workflow.setSignatureCompletedAt(LocalDateTime.now());

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheckInPaymentValidationResponseDto response = service.validatePayment(10L, "agent1");

        assertThat(response.isPassed()).isTrue();
        assertThat(workflow.getCurrentStep()).isEqualTo("COMPLETE_CHECKIN");
    }

    @Test
    void completeCheckInShouldBeIdempotentWhenAlreadyCompleted() {
        ReservationCheckInWorkflowRecord workflow = workflow("COMPLETE_CHECKIN");
        workflow.setCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        booking.setReservationStatus("CHECKED_IN");
        booking.setCheckInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        booking.setCheckInCompletedBy("frontdesk.user");

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));

        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));

        var response = service.completeCheckIn(10L, request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        verify(housekeepingRoomStatusService, never()).markOccupied(any());
    }

    @Test
    void completeCheckInShouldUpgradeArrivedToCheckedInWhenRequested() {
        ReservationCheckInWorkflowRecord workflow = workflow("COMPLETE_CHECKIN");
        workflow.setCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        booking.setReservationStatus("ARRIVED");
        booking.setCheckInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 0));
        booking.setCheckInCompletedBy("frontdesk.user");

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));

        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("supervisor.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));
        request.setTargetStatus("CHECKED_IN");

        var response = service.completeCheckIn(10L, request);

        assertThat(response.getReservationStatus()).isEqualTo("CHECKED_IN");
        verify(reservationBookingRepository).save(any(ReservationBookingRecord.class));
        verify(housekeepingRoomStatusService, never()).markOccupied(any());
    }

    @Test
    void getStepProgressShouldReturnProgressSummary() {
        ReservationCheckInWorkflowRecord workflow = workflow("ROOM_STAY");
        workflow.setGuestDetailsCompletedAt(LocalDateTime.now());

        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(workflowRepository.findByBookingId(10L)).thenReturn(Optional.of(workflow));
        when(signatureRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        CheckInStepProgressResponseDto response = service.getStepProgress(10L);

        assertThat(response.getCurrentStep()).isEqualTo("ROOM_STAY");
        assertThat(response.getCompletedSteps()).isEqualTo(1);
        assertThat(response.getTotalSteps()).isEqualTo(5);
        assertThat(response.getProgressPercent()).isEqualTo(20);
    }

    @Test
    void getAuditHistoryShouldReturnOrderedEvents() {
        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(auditRepository.findByBookingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(
                ReservationCheckInAuditRecord.builder()
                        .bookingId(10L)
                        .confirmationNumber("CONF-101")
                        .propertyId("PROP001")
                        .eventType("GUEST_DETAILS_UPDATED")
                        .eventMessage("Guest contact details updated during check-in")
                        .changedFields("personalEmail")
                        .actor("agent1")
                        .createdAt(LocalDateTime.of(2026, 7, 22, 9, 30))
                        .build(),
                ReservationCheckInAuditRecord.builder()
                        .bookingId(10L)
                        .confirmationNumber("CONF-101")
                        .propertyId("PROP001")
                        .eventType("ROOM_STAY_UPDATED")
                        .eventMessage("Room and stay details updated during check-in")
                        .changedFields("roomType")
                        .actor("agent2")
                        .createdAt(LocalDateTime.of(2026, 7, 22, 9, 40))
                        .build()
        ));

        var response = service.getAuditHistory(10L);

        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getEvents()).hasSize(2);
        assertThat(response.getEvents().get(0).getEventType()).isEqualTo("GUEST_DETAILS_UPDATED");
        assertThat(response.getEvents().get(1).getEventType()).isEqualTo("ROOM_STAY_UPDATED");
    }

        @Test
        void getAuditHistoryPageShouldApplyEventAndDateFilters() {
        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        ReservationCheckInAuditRecord record = ReservationCheckInAuditRecord.builder()
            .bookingId(10L)
            .confirmationNumber("CONF-101")
            .propertyId("PROP001")
            .eventType("SIGNATURE_CAPTURED")
            .eventMessage("Guest signature captured during check-in")
            .changedFields("contentType")
            .actor("agent1")
            .createdAt(LocalDateTime.of(2026, 7, 22, 9, 15))
            .build();

        Page<ReservationCheckInAuditRecord> page = new PageImpl<>(
            List.of(record),
            PageRequest.of(0, 10),
            1
        );

        when(auditRepository.findByBookingIdAndEventTypeIgnoreCaseAndCreatedAtBetween(
            eq(10L),
            eq("SIGNATURE_CAPTURED"),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any()
        )).thenReturn(page);

        var response = service.getAuditHistoryPage(
            10L,
            "SIGNATURE_CAPTURED",
            LocalDate.of(2026, 7, 20),
            LocalDate.of(2026, 7, 22),
            0,
            10,
            "desc"
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getEventType()).isEqualTo("SIGNATURE_CAPTURED");
        assertThat(response.getFilters().getEventType()).isEqualTo("SIGNATURE_CAPTURED");
        assertThat(response.getFilters().getFromDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(response.getFilters().getToDate()).isEqualTo(LocalDate.of(2026, 7, 22));
        }

        @Test
        void getAuditHistoryPageShouldRejectInvalidDateRange() {
        when(reservationBookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getAuditHistoryPage(
            10L,
            null,
            LocalDate.of(2026, 7, 23),
            LocalDate.of(2026, 7, 22),
            0,
            20,
            "desc"
        )).isInstanceOf(BadRequestException.class)
            .hasMessage("fromDate must be on or before toDate");
        }

    private ReservationCheckInWorkflowRecord workflow(String currentStep) {
        return ReservationCheckInWorkflowRecord.builder()
                .id(1L)
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .currentStep(currentStep)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
