package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.CheckInAuditHistoryResponseDto;
import com.pms.reservation.dto.CheckInAuditPageResponseDto;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.dto.CheckInGuestUpdateRequestDto;
import com.pms.reservation.dto.CheckInPaymentValidationResponseDto;
import com.pms.reservation.dto.CheckInRoomStayUpdateRequestDto;
import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.CheckInStepProgressResponseDto;
import com.pms.reservation.dto.CheckInWorkflowResponseDto;
import com.pms.reservation.service.ReservationCheckInWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/bookings/{bookingId}/check-in")
@RequiredArgsConstructor
@Tag(name = "Reservation Check-In Workflow", description = "Step-by-step check-in workflow APIs")
public class ReservationCheckInWorkflowController {

    private final ReservationCheckInWorkflowService workflowService;

    @GetMapping
    @Operation(summary = "Get check-in workflow state")
    public ResponseEntity<ApiResponse<CheckInWorkflowResponseDto>> getWorkflow(@PathVariable Long bookingId) {
        CheckInWorkflowResponseDto response = workflowService.getWorkflow(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Check-in workflow fetched successfully", response));
    }

    @GetMapping("/steps")
    @Operation(summary = "Get check-in step progress")
    public ResponseEntity<ApiResponse<CheckInStepProgressResponseDto>> getStepProgress(@PathVariable Long bookingId) {
        CheckInStepProgressResponseDto response = workflowService.getStepProgress(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Check-in step progress fetched successfully", response));
    }

    @GetMapping("/audit-history")
    @Operation(summary = "Get check-in audit history")
    public ResponseEntity<ApiResponse<CheckInAuditHistoryResponseDto>> getAuditHistory(@PathVariable Long bookingId) {
        CheckInAuditHistoryResponseDto response = workflowService.getAuditHistory(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Check-in audit history fetched successfully", response));
    }

    @GetMapping("/audit-history/page")
    @Operation(summary = "Get paginated check-in audit history with optional filters")
    public ResponseEntity<ApiResponse<CheckInAuditPageResponseDto>> getAuditHistoryPage(
            @PathVariable Long bookingId,
            @org.springframework.web.bind.annotation.RequestParam(name = "eventType", required = false) String eventType,
            @org.springframework.web.bind.annotation.RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        CheckInAuditPageResponseDto response = workflowService.getAuditHistoryPage(
                bookingId,
                eventType,
                fromDate,
                toDate,
                page,
                size,
                sortDir
        );
        return ResponseEntity.ok(ApiResponse.success("Check-in audit history page fetched successfully", response));
    }

    @PutMapping("/guest-details")
    @Operation(summary = "Update guest contact details")
    public ResponseEntity<ApiResponse<CheckInWorkflowResponseDto>> updateGuestDetails(
            @PathVariable Long bookingId,
            @Valid @RequestBody CheckInGuestUpdateRequestDto request,
            @RequestHeader(name = "X-Actor", required = false) String actor
    ) {
        CheckInWorkflowResponseDto response = workflowService.updateGuestDetails(bookingId, request, actor);
        return ResponseEntity.ok(ApiResponse.success("Guest details updated successfully", response));
    }

    @PutMapping("/room-stay")
    @Operation(summary = "Update room assignment and stay details")
    public ResponseEntity<ApiResponse<CheckInWorkflowResponseDto>> updateRoomStay(
            @PathVariable Long bookingId,
            @Valid @RequestBody CheckInRoomStayUpdateRequestDto request,
            @RequestHeader(name = "X-Actor", required = false) String actor
    ) {
        CheckInWorkflowResponseDto response = workflowService.updateRoomStay(bookingId, request, actor);
        return ResponseEntity.ok(ApiResponse.success("Room and stay details updated successfully", response));
    }

    @PutMapping("/signature")
    @Operation(summary = "Capture guest digital signature")
    public ResponseEntity<ApiResponse<CheckInWorkflowResponseDto>> saveSignature(
            @PathVariable Long bookingId,
            @Valid @RequestBody CheckInSignatureRequestDto request,
            @RequestHeader(name = "X-Actor", required = false) String actor
    ) {
        CheckInWorkflowResponseDto response = workflowService.saveSignature(bookingId, request, actor);
        return ResponseEntity.ok(ApiResponse.success("Signature captured successfully", response));
    }

    @PostMapping("/payment-validation")
    @Operation(summary = "Validate payment requirements before check-in")
    public ResponseEntity<ApiResponse<CheckInPaymentValidationResponseDto>> validatePayment(
            @PathVariable Long bookingId,
            @RequestHeader(name = "X-Actor", required = false) String actor
    ) {
        CheckInPaymentValidationResponseDto response = workflowService.validatePayment(bookingId, actor);
        return ResponseEntity.ok(ApiResponse.success("Payment validation completed", response));
    }

    @PostMapping("/complete")
    @Operation(summary = "Complete check-in")
    public ResponseEntity<ApiResponse<CheckInCompletionResponseDto>> completeCheckIn(
            @PathVariable Long bookingId,
            @Valid @RequestBody CheckInCompleteRequestDto request
    ) {
        CheckInCompletionResponseDto response = workflowService.completeCheckIn(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Check-in completed successfully", response));
    }

    @GetMapping("/signature")
    @Operation(summary = "Get stored signature")
    public ResponseEntity<ApiResponse<CheckInSignatureResponseDto>> getSignature(@PathVariable Long bookingId) {
        CheckInSignatureResponseDto response = workflowService.getSignature(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Signature fetched successfully", response));
    }
}
