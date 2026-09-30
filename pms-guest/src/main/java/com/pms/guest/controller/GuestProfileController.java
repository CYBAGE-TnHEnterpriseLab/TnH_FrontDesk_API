package com.pms.guest.controller;

import com.pms.guest.dto.request.GuestProfileCreateRequest;
import com.pms.guest.dto.request.GuestLookupRequest;
import com.pms.guest.dto.request.GuestProfileUpdateRequest;
import com.pms.guest.dto.response.GuestProfileResponse;
import com.pms.guest.service.GuestProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guests")
@Tag(name = "Guest Profiles", description = "APIs for managing property guest profiles")
@Validated
public class GuestProfileController {

    private final GuestProfileService guestProfileService;

    public GuestProfileController(GuestProfileService guestProfileService) {
        this.guestProfileService = guestProfileService;
    }

    @PostMapping("/lookup")
    @Operation(
            summary = "Find an existing guest profile using property-scoped identifiers",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GuestLookupRequest.class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "One matching guest profile",
                content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "No matching guest profile"),
        @ApiResponse(responseCode = "409", description = "Lookup matched multiple guest profiles"),
        @ApiResponse(responseCode = "400", description = "Invalid lookup request")
    })
    public ResponseEntity<GuestProfileResponse> findExistingGuest(
            @Valid @RequestBody GuestLookupRequest request
    ) {
        return guestProfileService.findExistingGuest(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "Create guest profile",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GuestProfileCreateRequest.class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Guest profile created",
                content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid guest profile request")
    })
    public ResponseEntity<GuestProfileResponse> createGuestProfile(
            @Valid @RequestBody GuestProfileCreateRequest request
    ) {
        GuestProfileResponse response = guestProfileService.createGuestProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guest profile by internal ID")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Guest profile found",
                content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Guest profile not found"),
        @ApiResponse(responseCode = "400", description = "propertyId is required")
    })
    public GuestProfileResponse getGuestProfileById(
            @Parameter(description = "Guest profile database ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Property ID used to scope the profile", required = true)
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId
    ) {
        return guestProfileService.getGuestProfileById(id, propertyId);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update guest profile",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = GuestProfileUpdateRequest.class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Guest profile updated",
                content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid guest profile request"),
        @ApiResponse(responseCode = "404", description = "Guest profile not found")
    })
    public GuestProfileResponse updateGuestProfile(
            @Parameter(description = "Guest profile database ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Property ID used to scope the profile", required = true)
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @Valid @RequestBody GuestProfileUpdateRequest request
    ) {
        return guestProfileService.updateGuestProfile(id, propertyId, request);
    }

    @GetMapping("/search")
    @Operation(summary = "Search guest profiles within a property")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Matching guest profiles",
                content = @Content(array = @ArraySchema(
                        schema = @Schema(implementation = GuestProfileResponse.class)))),
        @ApiResponse(responseCode = "400", description = "propertyId is required")
    })
    public List<GuestProfileResponse> searchGuestProfiles(
            @Parameter(description = "Property ID used to scope the search", required = true)
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @Parameter(description = "First name filter")
            @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter")
            @RequestParam(required = false) String lastName,
            @Parameter(description = "Phone number filter")
            @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Personal email filter")
            @RequestParam(required = false) String personalEmail,
            @Parameter(description = "Official email filter")
            @RequestParam(required = false) String officialEmail,
            @Parameter(description = "Loyalty membership number filter")
            @RequestParam(required = false) String loyaltyNumber
    ) {
        return guestProfileService.searchGuestProfiles(
                propertyId,
                firstName,
                lastName,
                phoneNumber,
                personalEmail,
                officialEmail,
                loyaltyNumber
        );
    }
}
