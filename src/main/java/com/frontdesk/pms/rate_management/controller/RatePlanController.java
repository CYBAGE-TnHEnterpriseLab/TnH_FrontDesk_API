package com.frontdesk.pms.rate_management.controller;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.enums.MealInclusion;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.service.RatePlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rate-plans")
public class RatePlanController {

    private final RatePlanService ratePlanService;

    public RatePlanController(RatePlanService ratePlanService) {
        this.ratePlanService = ratePlanService;
    }

    @PostMapping
    public ResponseEntity<RatePlanResponseDTO> createRatePlan(@RequestBody RatePlanRequestDTO requestDTO) {
        return ResponseEntity.ok(ratePlanService.createRatePlan(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RatePlanResponseDTO> updateRatePlan(@PathVariable Long id,
                                                              @RequestBody RatePlanRequestDTO requestDTO) {
        return ResponseEntity.ok(ratePlanService.updateRatePlan(id, requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RatePlanResponseDTO> getRatePlan(@PathVariable Long id) {
        return ResponseEntity.ok(ratePlanService.getRatePlan(id));
    }

    @GetMapping
    public List<RatePlanResponseDTO> getAllRatePlans() {
        return ratePlanService.getAllRatePlans();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RatePlanResponseDTO> updateStatus(@PathVariable Long id,
                                                            @RequestParam RatePlanStatus status) {
        return ResponseEntity.ok(ratePlanService.updateRatePlanStatus(id, status));
    }

    @GetMapping("/available")
    public List<RatePlanResponseDTO> getAvailableRatePlans(@RequestParam Long roomTypeId,
                                                            @RequestParam String occupancyType,
                                                            @RequestParam MealInclusion mealInclusion,
                                                            @RequestParam LocalDate stayDate) {
        return ratePlanService.getAvailableRatePlans(roomTypeId, occupancyType, mealInclusion, stayDate);
    }

    @GetMapping("/{id}/calculated-price")
    public ResponseEntity<RatePlanPriceResponseDTO> getCalculatedPrice(@PathVariable Long id,
                                                                       @RequestParam Long roomTypeId) {
        return ResponseEntity.ok(ratePlanService.calculatePriceFromMasterBar(id, roomTypeId));
    }
}
