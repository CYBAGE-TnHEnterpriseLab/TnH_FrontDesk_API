package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.service.ReservationCheckInWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/bookings/{confirmationNumber}/check-in")
@RequiredArgsConstructor
@Tag(name = "Reservation Check-In", description = "Reservation check-in API")
public class ReservationCheckInWorkflowController {

    private final ReservationCheckInWorkflowService workflowService;

    @PostMapping("/complete")
    @Operation(summary = "Complete check-in")
    public ResponseEntity<ApiResponse<CheckInCompletionResponseDto>> completeCheckIn(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody CheckInCompleteRequestDto request
    ) {
        CheckInCompletionResponseDto response = workflowService.completeCheckIn(confirmationNumber, request);
        return ResponseEntity.ok(ApiResponse.<CheckInCompletionResponseDto>builder()
                .success(true)
                .message("Check-in completed successfully")
                .data(response)
                .errors(Map.of())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

}
