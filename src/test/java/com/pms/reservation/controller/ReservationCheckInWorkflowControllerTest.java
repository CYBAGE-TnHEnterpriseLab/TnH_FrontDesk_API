package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.service.ReservationCheckInWorkflowService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReservationCheckInWorkflowService workflowService;

    @Test
    void completeCheckInShouldBeTheSingleCheckInEndpoint() throws Exception {
        CheckInCompleteRequestDto request = new CheckInCompleteRequestDto();
        request.setActor("frontdesk.user");
        request.setBusinessDate(LocalDate.of(2026, 7, 22));
        CheckInCompletionResponseDto response = CheckInCompletionResponseDto.builder()
                .confirmationNumber("CONF-101").reservationStatus("CHECKED_IN")
                .checkInCompletedBy("frontdesk.user")
                .checkInCompletedAt(LocalDateTime.of(2026, 7, 22, 10, 30)).build();
        when(workflowService.completeCheckIn(eq("CONF-101"), any(CheckInCompleteRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/bookings/CONF-101/check-in/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Check-in completed successfully"))
                .andExpect(jsonPath("$.data.confirmationNumber").value("CONF-101"))
                .andExpect(jsonPath("$.data.reservationStatus").value("CHECKED_IN"))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.endsWith("Z")));
    }
}
