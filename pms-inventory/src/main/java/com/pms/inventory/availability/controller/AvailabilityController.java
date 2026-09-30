package com.pms.inventory.availability.controller;

import com.pms.inventory.availability.dto.AvailabilityResponse;
import com.pms.inventory.availability.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/availability")
@Validated
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(summary = "Get room-type availability")
    public List<AvailabilityResponse> getAvailability(
            @RequestParam @NotNull String propertyId,
            @RequestParam @NotNull String roomTypeId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return availabilityService.getAvailability(propertyId, roomTypeId, fromDate, toDate);
    }
}

