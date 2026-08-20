package com.frontdesk.pms.rate_management.controller;

import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
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

    // Create a new rate plan for the given property.
    @PostMapping("/create-rate-plan/property/{propertyId}")
    public ResponseEntity<RatePlanResponseDTO> createRatePlan(@PathVariable String propertyId,
            @RequestBody RatePlanRequestDTO requestDTO) {
        return ResponseEntity.ok(ratePlanService.createRatePlan(propertyId, requestDTO));
    }

    // Update an existing rate plan for the given property.
    @PutMapping("/update-rate-plan/property/{propertyId}/{id}")
    public ResponseEntity<RatePlanResponseDTO> updateRatePlan(@PathVariable String propertyId,
            @PathVariable Long id,
            @RequestBody RatePlanRequestDTO requestDTO) {
        return ResponseEntity.ok(ratePlanService.updateRatePlan(propertyId, id, requestDTO));
    }

    // List all rate plans configured for the given property.
    @GetMapping("/get-all-rate-plans/property/{propertyId}")
    public List<RatePlanResponseDTO> getAllRatePlans(@PathVariable String propertyId) {
        return ratePlanService.getAllRatePlans(propertyId);
    }

    // Delete one rate plan from the given property.
    @DeleteMapping("/delete-rate-plan/property/{propertyId}/{id}")
    public ResponseEntity<Void> deleteRatePlan(@PathVariable String propertyId,
            @PathVariable Long id) {
        ratePlanService.deleteRatePlan(propertyId, id);
        return ResponseEntity.noContent().build();
    }

    // // Get one rate plan by id within the given property.
    // @GetMapping("/{id}")
    // public ResponseEntity<RatePlanResponseDTO> getRatePlan(@PathVariable String
    // propertyId,
    // @PathVariable Long id) {
    // return ResponseEntity.ok(ratePlanService.getRatePlan(propertyId, id));
    // }

    // Change the status (ACTIVE/INACTIVE) of a rate plan in the given property.
    @PatchMapping("/change-rate-plan-status/property/{propertyId}/{id}/status")
    public ResponseEntity<RatePlanResponseDTO> updateStatus(@PathVariable String propertyId,
            @PathVariable Long id,
            @RequestParam RatePlanStatus status) {
        return ResponseEntity.ok(ratePlanService.updateRatePlanStatus(propertyId, id, status));
    }

    // Fetch available rate plans for a room type/occupancy/meal/date in the given
    // property.
    @GetMapping("/available/property/{propertyId}")
    public List<RatePlanResponseDTO> getAvailableRatePlans(@PathVariable String propertyId,
            @RequestParam Long roomTypeId,
            @RequestParam String occupancyType,
            @RequestParam MasterRoomMealOption mealOption,
            @RequestParam LocalDate stayDate) {
        return ratePlanService.getAvailableRatePlans(propertyId, roomTypeId, occupancyType, mealOption, stayDate);
    }

    // Calculate final price for a rate plan and room type in the given property.
    @GetMapping("/final-price/property/{propertyId}/{id}/calculated-price")
    public ResponseEntity<RatePlanPriceResponseDTO> getCalculatedPrice(@PathVariable String propertyId,
            @PathVariable Long id,
            @RequestParam Long roomTypeId,
            @RequestParam(required = false) Integer guestCount,
            @RequestParam(required = false) String occupancyType) {
        String occupancyTypeInput = occupancyType;
        if ((occupancyTypeInput == null || occupancyTypeInput.isBlank()) && guestCount != null) {
    
            occupancyTypeInput = guestCount + " Guest";
        }

        return ResponseEntity.ok(
                ratePlanService.calculatePriceFromMasterBar(propertyId, id, roomTypeId, occupancyTypeInput));
    }

    @PatchMapping("/map/policy-to-rate-plan/property/{propertyId}/{ratePlanId}")
    public ResponseEntity<RatePlanResponseDTO> mapPolicyToRatePlan(@PathVariable String propertyId,
            @PathVariable Long ratePlanId,
            @RequestParam List<String> policyId) {
        return ResponseEntity.ok(ratePlanService.mapPolicyToRatePlan(propertyId, ratePlanId, policyId));
    }

    // Calculate final price for a rate plan and room type in the given property.
    @PatchMapping("/unmap/policy-from-rate-plan/property/{propertyId}/{ratePlanId}")
    public ResponseEntity<RatePlanResponseDTO> unmapPolicyToRatePlan(@PathVariable String propertyId,
            @PathVariable Long ratePlanId,
            @RequestParam List<String> policyId) {
        return ResponseEntity.ok(ratePlanService.unmapPolicyToRatePlan(propertyId, ratePlanId, policyId));
    }
}
