package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.LoyaltyVerificationResponse;
import com.pms.reservation.service.LoyaltyVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guest endpoints reachable through the API gateway, which routes /api/v1/guests/** to this service.
 */
@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
@Validated
@Tag(name = "Guest Loyalty", description = "Verify a guest loyalty number and fetch the guest it belongs to")
public class GuestLoyaltyController {

    private final LoyaltyVerificationService loyaltyVerificationService;

    @GetMapping("/loyalty-verification")
    @Operation(summary = "Verify a loyalty number",
            description = "Validates a loyalty number and returns the guest details associated with it. "
                    + "Responds 404 when the loyalty number is invalid or no guest profile is found for it.")
    public ResponseEntity<ApiResponse<LoyaltyVerificationResponse>> verifyLoyaltyNumber(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam @NotBlank(message = "loyaltyNumber is required") String loyaltyNumber
    ) {
        LoyaltyVerificationResponse response =
                loyaltyVerificationService.verifyLoyaltyNumber(propertyId, loyaltyNumber);
        return ResponseEntity.ok(ApiResponse.success("Loyalty number verified successfully", response));
    }
}
