package com.pms.guest.controller;

import com.pms.guest.dto.response.GuestDetailsResponse;
import com.pms.guest.service.GuestDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guests")
@Tag(name = "Guest Details", description = "Read-only guest details with reservation context")
@Validated
public class GuestDetailsController {

    private final GuestDetailsService guestDetailsService;

    public GuestDetailsController(GuestDetailsService guestDetailsService) {
        this.guestDetailsService = guestDetailsService;
    }

    @GetMapping("/details")
    @Operation(summary = "Get guest details by exactly one of phoneNumber, email, bookingId or confirmationNumber")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "All matching guest-booking assignments (possibly empty)",
                content = @Content(schema = @Schema(implementation = GuestDetailsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing propertyId or not exactly one search criterion"),
        @ApiResponse(responseCode = "502", description = "Reservation service unavailable")
    })
    public GuestDetailsResponse getGuestDetails(
            @Parameter(description = "Property ID used to scope the search", required = true)
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @Parameter(description = "Exact match against phoneNumber or mobileNumber")
            @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Exact, case-sensitive match against personalEmail or officialEmail")
            @RequestParam(required = false) String email,
            @Parameter(description = "Reservation booking ID")
            @RequestParam(required = false) Long bookingId,
            @Parameter(description = "Reservation confirmation number (may span multiple bookings)")
            @RequestParam(required = false) String confirmationNumber
    ) {
        return guestDetailsService.getGuestDetails(propertyId, phoneNumber, email, bookingId, confirmationNumber);
    }
}
