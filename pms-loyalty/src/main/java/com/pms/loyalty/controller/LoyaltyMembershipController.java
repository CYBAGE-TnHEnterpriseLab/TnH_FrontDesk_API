package com.pms.loyalty.controller;

import com.pms.loyalty.dto.ApiResponse;
import com.pms.loyalty.dto.request.EnsureLoyaltyMembershipRequest;
import com.pms.loyalty.dto.response.LoyaltyMembershipResponse;
import com.pms.loyalty.service.LoyaltyEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty/internal/memberships")
@RequiredArgsConstructor
@Tag(name = "Loyalty Membership", description = "Internal APIs for loyalty membership management")
public class LoyaltyMembershipController {

    private final LoyaltyEnrollmentService loyaltyEnrollmentService;

    @PostMapping("/ensure")
    @Operation(summary = "Ensure loyalty membership",
            description = "Ensures a guest has a loyalty membership for the given program. Enrolls if not already a member.")
    public ResponseEntity<ApiResponse<LoyaltyMembershipResponse>> ensureMembership(
            @Valid @RequestBody EnsureLoyaltyMembershipRequest request) {
        LoyaltyMembershipResponse response = loyaltyEnrollmentService.ensureMembership(
                request.getGuestId(), request.getLoyaltyProgramId());
        return ResponseEntity.ok(ApiResponse.success("Membership ensured successfully", response));
    }
}
