package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.CheckInAuditEventDto;
import com.pms.reservation.dto.CheckInAuditHistoryResponseDto;
import com.pms.reservation.dto.CheckInAuditFilterRequestDto;
import com.pms.reservation.dto.CheckInAuditPageResponseDto;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.dto.CheckInGuestUpdateRequestDto;
import com.pms.reservation.dto.CheckInPaymentValidationResponseDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.CheckInStepProgressResponseDto;
import com.pms.reservation.dto.CheckInWorkflowResponseDto;
import com.pms.reservation.service.ReservationCheckInWorkflowService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationCheckInWorkflowController.class, properties = "security.jwt.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationCheckInWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationCheckInWorkflowService workflowService;

    @Test
    void getWorkflowShouldReturnWorkflowState() throws Exception {
        CheckInWorkflowResponseDto response = CheckInWorkflowResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .currentStep("GUEST_DETAILS")
                .progressPercent(0)
                .build();

        when(workflowService.getWorkflow("CONF-101")).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/bookings/CONF-101/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(10))
                .andExpect(jsonPath("$.data.currentStep").value("GUEST_DETAILS"));

        verify(workflowService).getWorkflow("CONF-101");
    }

    @Test
    void getStepProgressShouldReturnStepData() throws Exception {
        CheckInStepProgressResponseDto response = CheckInStepProgressResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .currentStep("ROOM_STAY")
                .progressPercent(20)
                .completedSteps(1)
                .totalSteps(5)
                .build();

        when(workflowService.getStepProgress("CONF-101")).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/bookings/CONF-101/check-in/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStep").value("ROOM_STAY"))
                .andExpect(jsonPath("$.data.progressPercent").value(20));
    }

    @Test
    void getAuditHistoryShouldReturnEvents() throws Exception {
        CheckInAuditHistoryResponseDto response = CheckInAuditHistoryResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .totalEvents(1)
                .events(List.of(CheckInAuditEventDto.builder()
                        .eventType("GUEST_DETAILS_UPDATED")
                        .eventMessage("Guest contact details updated during check-in")
                        .changedFields("personalEmail")
                        .actor("agent1")
                        .createdAt(LocalDateTime.of(2026, 7, 22, 10, 0))
                        .build()))
                .build();

        when(workflowService.getAuditHistory("CONF-101")).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/bookings/CONF-101/check-in/audit-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.events[0].eventType").value("GUEST_DETAILS_UPDATED"));
    }

    @Test
    void getAuditHistoryPageShouldReturnPaginatedResponse() throws Exception {
        CheckInAuditPageResponseDto response = CheckInAuditPageResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .propertyId("PROP001")
                .filters(CheckInAuditFilterRequestDto.builder()
                        .eventType("SIGNATURE_CAPTURED")
                        .fromDate(LocalDate.of(2026, 7, 20))
                        .toDate(LocalDate.of(2026, 7, 22))
                        .page(0)
                        .size(10)
                        .sortDir("desc")
                        .build())
                .events(List.of(CheckInAuditEventDto.builder()
                        .eventType("SIGNATURE_CAPTURED")
                        .eventMessage("Guest signature captured during check-in")
                        .changedFields("contentType")
                        .actor("agent1")
                        .createdAt(LocalDateTime.of(2026, 7, 22, 10, 5))
                        .build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(workflowService.getAuditHistoryPage(
                eq("CONF-101"),
                eq("SIGNATURE_CAPTURED"),
                eq(LocalDate.of(2026, 7, 20)),
                eq(LocalDate.of(2026, 7, 22)),
                eq(0),
                eq(10),
                eq("desc")
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/bookings/CONF-101/check-in/audit-history/page")
                        .queryParam("eventType", "SIGNATURE_CAPTURED")
                        .queryParam("fromDate", "2026-07-20")
                        .queryParam("toDate", "2026-07-22")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.events[0].eventType").value("SIGNATURE_CAPTURED"));
    }

    @Test
    void updateGuestDetailsShouldValidateMandatoryFields() throws Exception {
        CheckInGuestUpdateRequestDto request = new CheckInGuestUpdateRequestDto();
        request.setPersonalEmail(" ");

        mockMvc.perform(put("/api/v1/reservations/bookings/CONF-101/check-in/guest-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void validatePaymentShouldReturnSuccess() throws Exception {
        CheckInPaymentValidationResponseDto response = CheckInPaymentValidationResponseDto.builder()
                .passed(true)
                .message("Payment validation passed")
                .build();

        when(workflowService.validatePayment(eq("CONF-101"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/bookings/CONF-101/check-in/payment-validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    void completeCheckInShouldReturnCompletionPayload() throws Exception {
        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));
        request.setTargetStatus("ARRIVED");

        CheckInCompletionResponseDto response = CheckInCompletionResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .reservationStatus("ARRIVED")
                .checkInCompletedBy("frontdesk.user")
                .checkInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 30))
                .build();

        when(workflowService.completeCheckIn(eq("CONF-101"), any(CheckInCompleteRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/bookings/CONF-101/check-in/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationStatus").value("ARRIVED"));
    }

    @Test
    void getSignatureShouldReturnSavedSignature() throws Exception {
        CheckInSignatureResponseDto response = CheckInSignatureResponseDto.builder()
                .bookingId(10L)
                .confirmationNumber("CONF-101")
                .contentType("image/png")
                .payloadBase64("abc123")
                .signedAt(LocalDateTime.of(2026, 7, 22, 9, 0))
                .build();

        when(workflowService.getSignature("CONF-101")).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/bookings/CONF-101/check-in/signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contentType").value("image/png"));
    }
}
