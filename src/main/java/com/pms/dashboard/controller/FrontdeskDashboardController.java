package com.pms.dashboard.controller;

import com.pms.dashboard.dto.response.FrontdeskDashboardResponse;
import com.pms.dashboard.service.FrontdeskDashboardService;
import com.pms.guestlisting.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/frontdesk")
@Tag(name = "Frontdesk Dashboard", description = "Aggregated dashboard API")
@Validated
public class FrontdeskDashboardController {

    private final FrontdeskDashboardService dashboardService;

    public FrontdeskDashboardController(FrontdeskDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get frontdesk dashboard by property and business date")
    public ResponseEntity<ApiResponse<FrontdeskDashboardResponse>> getDashboard(
            @RequestParam @NotNull UUID propertyId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched successfully", dashboardService.getDashboard(propertyId, businessDate)));
    }
}

