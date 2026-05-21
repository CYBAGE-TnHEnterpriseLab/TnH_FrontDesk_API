package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MealInclusion;
import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import com.frontdesk.pms.rate_management.exception.RatePlanNotFoundException;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.RatePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatePlanService {

    private final RatePlanRepository ratePlanRepository;
    private final MasterRoomPricingRepository masterRoomPricingRepository;

    @Transactional
    public RatePlanResponseDTO createRatePlan(RatePlanRequestDTO requestDTO) {
        validateRatePlanRequest(requestDTO);
        if (ratePlanRepository.existsByCodeIgnoreCase(requestDTO.getCode())) {
            throw new InvalidRatePlanException("Rate code already exists: " + requestDTO.getCode());
        }

        validateNoOverlapForActivePlan(
            requestDTO.getApplicableRoomTypeIds(),
            requestDTO.getOccupancyType(),
            requestDTO.getMealInclusion(),
            requestDTO.getStartDate(),
            requestDTO.getEndDate(),
            null);

        RatePlan ratePlan = toEntity(requestDTO);
        ratePlan.setStatus(RatePlanStatus.ACTIVE);
        return toResponseDTO(ratePlanRepository.save(ratePlan));
    }

    @Transactional
    public RatePlanResponseDTO updateRatePlan(Long id, RatePlanRequestDTO requestDTO) {
        validateRatePlanRequest(requestDTO);
        RatePlan existing = ratePlanRepository.findById(id).orElseThrow(() -> new RatePlanNotFoundException(id));

        if (!existing.getCode().equalsIgnoreCase(requestDTO.getCode())
                && ratePlanRepository.existsByCodeIgnoreCase(requestDTO.getCode())) {
            throw new InvalidRatePlanException("Rate code already exists: " + requestDTO.getCode());
        }

        existing.setName(requestDTO.getName());
        existing.setCode(requestDTO.getCode());
        existing.setOccupancyType(requestDTO.getOccupancyType());
        existing.setMealInclusion(requestDTO.getMealInclusion());
        existing.setType(requestDTO.getType());
        existing.setStartDate(requestDTO.getStartDate());
        existing.setEndDate(requestDTO.getEndDate());
        existing.setCalculationMethod(requestDTO.getCalculationMethod());
        existing.setAdjustmentValue(requestDTO.getAdjustmentValue());
        existing.setManualAmount(requestDTO.getManualAmount());
        existing.setApplicableRoomTypeIds(new HashSet<>(requestDTO.getApplicableRoomTypeIds()));

        if (existing.getStatus() == RatePlanStatus.ACTIVE) {
            validateNoOverlapForActivePlan(
                    existing.getApplicableRoomTypeIds(),
                    existing.getOccupancyType(),
                    existing.getMealInclusion(),
                    existing.getStartDate(),
                    existing.getEndDate(),
                    existing.getId());
        }

        return toResponseDTO(ratePlanRepository.save(existing));
    }

    public RatePlanResponseDTO getRatePlan(Long id) {
        return ratePlanRepository.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RatePlanNotFoundException(id));
    }

    public List<RatePlanResponseDTO> getAllRatePlans() {
        return ratePlanRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RatePlanResponseDTO updateRatePlanStatus(Long id, RatePlanStatus status) {
        RatePlan ratePlan = ratePlanRepository.findById(id).orElseThrow(() -> new RatePlanNotFoundException(id));

        if (status == RatePlanStatus.ACTIVE) {
            validateNoOverlapForActivePlan(
                    ratePlan.getApplicableRoomTypeIds(),
                    ratePlan.getOccupancyType(),
                    ratePlan.getMealInclusion(),
                    ratePlan.getStartDate(),
                    ratePlan.getEndDate(),
                    ratePlan.getId());
        }

        ratePlan.setStatus(status);
        return toResponseDTO(ratePlanRepository.save(ratePlan));
    }

    public List<RatePlanResponseDTO> getAvailableRatePlans(Long roomTypeId,
                                                            String occupancyType,
                                                            MealInclusion mealInclusion,
                                                            LocalDate stayDate) {
        return ratePlanRepository.findAvailableByRoomTypeOccupancyMealAndDate(
                        roomTypeId,
                        occupancyType,
                        mealInclusion,
                        stayDate,
                        RatePlanStatus.ACTIVE)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public RatePlanPriceResponseDTO calculatePriceFromMasterBar(Long ratePlanId, Long roomTypeId) {
        RatePlan ratePlan = ratePlanRepository.findById(ratePlanId)
                .orElseThrow(() -> new RatePlanNotFoundException(ratePlanId));

        Double masterBarAmount = null;
        Double finalAmount;
        switch (ratePlan.getCalculationMethod()) {
            case MANUAL -> {
                finalAmount = ratePlan.getManualAmount();
            }
            case PERCENT_OFF_BAR -> {
                masterBarAmount = resolveMasterBarAmount(ratePlan, roomTypeId);
                finalAmount = masterBarAmount - (masterBarAmount * ratePlan.getAdjustmentValue() / 100.0);
            }
            case PERCENT_ADD_BAR -> {
                masterBarAmount = resolveMasterBarAmount(ratePlan, roomTypeId);
                finalAmount = masterBarAmount + (masterBarAmount * ratePlan.getAdjustmentValue() / 100.0);
            }
            case FLAT_OFF_BAR -> {
                masterBarAmount = resolveMasterBarAmount(ratePlan, roomTypeId);
                finalAmount = masterBarAmount - ratePlan.getAdjustmentValue();
            }
            case FLAT_ADD_BAR -> {
                masterBarAmount = resolveMasterBarAmount(ratePlan, roomTypeId);
                finalAmount = masterBarAmount + ratePlan.getAdjustmentValue();
            }
            default -> throw new InvalidRatePlanException("Unsupported calculation method");
        }

        RatePlanPriceResponseDTO responseDTO = new RatePlanPriceResponseDTO();
        responseDTO.setRatePlanId(ratePlanId);
        responseDTO.setMasterBarAmount(masterBarAmount);
        responseDTO.setFinalAmount(Math.max(0.0, finalAmount));
        return responseDTO;
    }

    private void validateRatePlanRequest(RatePlanRequestDTO requestDTO) {
        if (requestDTO.getName() == null || requestDTO.getName().isBlank()) {
            throw new InvalidRatePlanException("Rate plan name is required");
        }
        if (requestDTO.getCode() == null || requestDTO.getCode().isBlank()) {
            throw new InvalidRatePlanException("Rate code is required");
        }
        if (requestDTO.getOccupancyType() == null || requestDTO.getOccupancyType().isBlank()) {
            throw new InvalidRatePlanException("Occupancy type is required");
        }
        if (requestDTO.getType() == null) {
            throw new InvalidRatePlanException("Rate plan type is required");
        }
        if (requestDTO.getMealInclusion() == null) {
            throw new InvalidRatePlanException("Meal inclusion is required");
        }
        if (requestDTO.getCalculationMethod() == null) {
            throw new InvalidRatePlanException("Calculation method is required");
        }
        if (requestDTO.getStartDate() == null || requestDTO.getEndDate() == null) {
            throw new InvalidRatePlanException("Start date and end date are required");
        }
        if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())) {
            throw new InvalidRatePlanException("End date cannot be before start date");
        }
        if (requestDTO.getApplicableRoomTypeIds() == null || requestDTO.getApplicableRoomTypeIds().isEmpty()) {
            throw new InvalidRatePlanException("At least one room type must be selected");
        }

        if (requestDTO.getCalculationMethod() == RatePlanCalculationMethod.MANUAL) {
            if (requestDTO.getManualAmount() == null || requestDTO.getManualAmount() < 0) {
                throw new InvalidRatePlanException("Manual amount must be provided and cannot be negative");
            }
            return;
        }

        if (requestDTO.getAdjustmentValue() == null || requestDTO.getAdjustmentValue() < 0) {
            throw new InvalidRatePlanException("Adjustment value must be provided and cannot be negative");
        }

        if (requestDTO.getCalculationMethod() == RatePlanCalculationMethod.PERCENT_OFF_BAR
                && requestDTO.getAdjustmentValue() > 100) {
            throw new InvalidRatePlanException("Percent off cannot be greater than 100");
        }
    }

    private RatePlan toEntity(RatePlanRequestDTO dto) {
        RatePlan entity = new RatePlan();
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setOccupancyType(dto.getOccupancyType());
        entity.setMealInclusion(dto.getMealInclusion());
        entity.setType(dto.getType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCalculationMethod(dto.getCalculationMethod());
        entity.setAdjustmentValue(dto.getAdjustmentValue());
        entity.setManualAmount(dto.getManualAmount());
        entity.setApplicableRoomTypeIds(new HashSet<>(dto.getApplicableRoomTypeIds()));
        return entity;
    }

    private RatePlanResponseDTO toResponseDTO(RatePlan entity) {
        RatePlanResponseDTO dto = new RatePlanResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setOccupancyType(entity.getOccupancyType());
        dto.setMealInclusion(entity.getMealInclusion());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setApplicableRoomTypeIds(entity.getApplicableRoomTypeIds());
        dto.setCalculationMethod(entity.getCalculationMethod());
        dto.setAdjustmentValue(entity.getAdjustmentValue());
        dto.setManualAmount(entity.getManualAmount());
        return dto;
    }

    private void validateMasterBarAmount(Double masterBarAmount) {
        if (masterBarAmount == null || masterBarAmount < 0) {
            throw new InvalidRatePlanException("Master BAR amount must be provided and cannot be negative");
        }
    }

    private Double resolveMasterBarAmount(RatePlan ratePlan, Long roomTypeId) {
        if (roomTypeId == null) {
            throw new InvalidRatePlanException("Room type id is required to derive BAR amount");
        }

        if (ratePlan.getApplicableRoomTypeIds() == null || !ratePlan.getApplicableRoomTypeIds().contains(roomTypeId)) {
            throw new InvalidRatePlanException("Room type is not applicable for this rate plan");
        }

        MasterRoomPricing pricing = masterRoomPricingRepository
                .findByRoomTypeIdAndOccupancyType(roomTypeId, ratePlan.getOccupancyType())
                .orElseThrow(() -> new InvalidRatePlanException(
                        "Master BAR pricing not found for room type " + roomTypeId
                                + " and occupancy " + ratePlan.getOccupancyType()));

        validateMasterBarAmount(pricing.getPrice());
        return pricing.getPrice();
    }

    private void validateNoOverlapForActivePlan(java.util.Set<Long> roomTypeIds,
                                                String occupancyType,
                                                MealInclusion mealInclusion,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Long excludeRatePlanId) {
        long overlappingPlanCount = ratePlanRepository.countOverlappingActivePlans(
                roomTypeIds,
                occupancyType,
                mealInclusion,
                startDate,
                endDate,
                RatePlanStatus.ACTIVE,
                excludeRatePlanId);

        if (overlappingPlanCount > 0) {
            throw new InvalidRatePlanException(
                    "Overlapping active rate plan exists for same room type, occupancy, meal inclusion, and date range");
        }
    }
}
