package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MealInclusion;
import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import com.frontdesk.pms.rate_management.exception.RatePlanNotFoundException;
import com.frontdesk.pms.rate_management.exception.PropertyNotFoundException;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.RatePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatePlanService {

    private final RatePlanRepository ratePlanRepository;
    private final MasterRoomPricingRepository masterRoomPricingRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final RoomServiceClient roomServiceClient;

    @Transactional
    public RatePlanResponseDTO createRatePlan(String propertyId, RatePlanRequestDTO requestDTO) {
        validateProperty(propertyId);
        validateRatePlanRequest(propertyId, requestDTO, null);
        if (ratePlanRepository.existsByPropertyIdAndCodeIgnoreCase(propertyId, requestDTO.getCode())) {
            throw new InvalidRatePlanException("Rate code already exists: " + requestDTO.getCode());
        }

        validateNoOverlapForActivePlan(
            propertyId,
            requestDTO.getApplicableRoomTypeIds(),
            requestDTO.getOccupancyType(),
            requestDTO.getMealInclusion(),
            requestDTO.getStartDate(),
            requestDTO.getEndDate(),
            null);

        RatePlan ratePlan = toEntity(propertyId, requestDTO);
        ratePlan.setStatus(RatePlanStatus.ACTIVE);
        return toResponseDTO(ratePlanRepository.save(ratePlan));
    }

    @Transactional
    public RatePlanResponseDTO updateRatePlan(String propertyId, Long id, RatePlanRequestDTO requestDTO) {
        validateProperty(propertyId);
        validateRatePlanRequest(propertyId, requestDTO, id);
        RatePlan existing = ratePlanRepository.findByIdAndPropertyId(id, propertyId)
                .orElseThrow(() -> new RatePlanNotFoundException(id));

        if (!existing.getCode().equalsIgnoreCase(requestDTO.getCode())
                && ratePlanRepository.existsByPropertyIdAndCodeIgnoreCase(propertyId, requestDTO.getCode())) {
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
        existing.setParentRatePlanId(requestDTO.getParentRatePlanId());
        existing.setApplicableRoomTypeIds(new HashSet<>(requestDTO.getApplicableRoomTypeIds()));

        if (existing.getStatus() == RatePlanStatus.ACTIVE) {
            validateNoOverlapForActivePlan(
                    propertyId,
                    existing.getApplicableRoomTypeIds(),
                    existing.getOccupancyType(),
                    existing.getMealInclusion(),
                    existing.getStartDate(),
                    existing.getEndDate(),
                    existing.getId());
        }

        return toResponseDTO(ratePlanRepository.save(existing));
    }

    public RatePlanResponseDTO getRatePlan(String propertyId, Long id) {
        validateProperty(propertyId);
        return ratePlanRepository.findByIdAndPropertyId(id, propertyId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RatePlanNotFoundException(id));
    }

    public List<RatePlanResponseDTO> getAllRatePlans(String propertyId) {
        validateProperty(propertyId);
        return ratePlanRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RatePlanResponseDTO updateRatePlanStatus(String propertyId, Long id, RatePlanStatus status) {
        validateProperty(propertyId);
        RatePlan ratePlan = ratePlanRepository.findByIdAndPropertyId(id, propertyId)
                .orElseThrow(() -> new RatePlanNotFoundException(id));

        if (status == RatePlanStatus.ACTIVE) {
            validateNoOverlapForActivePlan(
                    propertyId,
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

    public List<RatePlanResponseDTO> getAvailableRatePlans(String propertyId,
                                                            Long roomTypeId,
                                                            String occupancyType,
                                                            MealInclusion mealInclusion,
                                                            LocalDate stayDate) {
        validateProperty(propertyId);
        validateRoomTypeBelongsToProperty(propertyId, roomTypeId);
        return ratePlanRepository.findAvailableByRoomTypeOccupancyMealAndDate(
                        propertyId,
                        roomTypeId,
                        occupancyType,
                        mealInclusion,
                        stayDate,
                        RatePlanStatus.ACTIVE)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public RatePlanPriceResponseDTO calculatePriceFromMasterBar(String propertyId, Long ratePlanId, Long roomTypeId) {
        validateProperty(propertyId);
        validateRoomTypeBelongsToProperty(propertyId, roomTypeId);
        CalculationResult calculation = calculateRatePlanAmount(propertyId, ratePlanId, roomTypeId, new HashSet<>());

        RatePlanPriceResponseDTO responseDTO = new RatePlanPriceResponseDTO();
        responseDTO.setRatePlanId(ratePlanId);
        responseDTO.setMasterBarAmount(calculation.masterBarAmount());
        responseDTO.setFinalAmount(calculation.finalAmount());
        return responseDTO;
    }

    private void validateRatePlanRequest(String propertyId, RatePlanRequestDTO requestDTO, Long currentRatePlanId) {
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
        validateRoomTypesBelongToProperty(propertyId, requestDTO.getApplicableRoomTypeIds());

        if (requestDTO.getParentRatePlanId() != null) {
            if (requestDTO.getParentRatePlanId().equals(currentRatePlanId)) {
                throw new InvalidRatePlanException("Rate plan cannot derive from itself");
            }

            RatePlan parentRatePlan = ratePlanRepository.findByIdAndPropertyId(requestDTO.getParentRatePlanId(), propertyId)
                    .orElseThrow(() -> new InvalidRatePlanException(
                            "Parent rate plan not found: " + requestDTO.getParentRatePlanId()));

            if (!parentRatePlan.getOccupancyType().equals(requestDTO.getOccupancyType())) {
                throw new InvalidRatePlanException("Parent and child rate plans must have the same occupancy type");
            }
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

    private RatePlan toEntity(String propertyId, RatePlanRequestDTO dto) {
        RatePlan entity = new RatePlan();
        entity.setPropertyId(propertyId);
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
        entity.setParentRatePlanId(dto.getParentRatePlanId());
        entity.setApplicableRoomTypeIds(new HashSet<>(dto.getApplicableRoomTypeIds()));
        return entity;
    }

    private RatePlanResponseDTO toResponseDTO(RatePlan entity) {
        RatePlanResponseDTO dto = new RatePlanResponseDTO();
        dto.setId(entity.getId());
        dto.setPropertyId(entity.getPropertyId());
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
        dto.setParentRatePlanId(entity.getParentRatePlanId());
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

    private CalculationResult calculateRatePlanAmount(String propertyId, Long ratePlanId, Long roomTypeId, Set<Long> visitedRatePlanIds) {
        if (!visitedRatePlanIds.add(ratePlanId)) {
            throw new InvalidRatePlanException("Circular rate plan derivation detected");
        }

        RatePlan ratePlan = ratePlanRepository.findByIdAndPropertyId(ratePlanId, propertyId)
                .orElseThrow(() -> new RatePlanNotFoundException(ratePlanId));

        if (ratePlan.getApplicableRoomTypeIds() == null || !ratePlan.getApplicableRoomTypeIds().contains(roomTypeId)) {
            throw new InvalidRatePlanException("Room type is not applicable for this rate plan");
        }

        try {
            return switch (ratePlan.getCalculationMethod()) {
                case MANUAL -> new CalculationResult(null, Math.max(0.0, ratePlan.getManualAmount()));
                case PERCENT_OFF_BAR, PERCENT_ADD_BAR, FLAT_OFF_BAR, FLAT_ADD_BAR -> {
                    CalculationResult baseCalculation = resolveBaseCalculation(propertyId, ratePlan, roomTypeId, visitedRatePlanIds);
                    Double baseAmount = baseCalculation.finalAmount();
                    Double derivedFinalAmount = applyAdjustment(ratePlan, baseAmount);
                    yield new CalculationResult(baseCalculation.masterBarAmount(), Math.max(0.0, derivedFinalAmount));
                }
            };
        } finally {
            visitedRatePlanIds.remove(ratePlanId);
        }
    }

    private CalculationResult resolveBaseCalculation(String propertyId, RatePlan ratePlan, Long roomTypeId, Set<Long> visitedRatePlanIds) {
        if (ratePlan.getParentRatePlanId() != null) {
            CalculationResult parentCalculation = calculateRatePlanAmount(
                    propertyId,
                    ratePlan.getParentRatePlanId(),
                    roomTypeId,
                    visitedRatePlanIds);
            return new CalculationResult(parentCalculation.masterBarAmount(), parentCalculation.finalAmount());
        }

        Double masterBarAmount = resolveMasterBarAmount(ratePlan, roomTypeId);
        return new CalculationResult(masterBarAmount, masterBarAmount);
    }

    private Double applyAdjustment(RatePlan ratePlan, Double baseAmount) {
        return switch (ratePlan.getCalculationMethod()) {
            case PERCENT_OFF_BAR -> baseAmount - (baseAmount * ratePlan.getAdjustmentValue() / 100.0);
            case PERCENT_ADD_BAR -> baseAmount + (baseAmount * ratePlan.getAdjustmentValue() / 100.0);
            case FLAT_OFF_BAR -> baseAmount - ratePlan.getAdjustmentValue();
            case FLAT_ADD_BAR -> baseAmount + ratePlan.getAdjustmentValue();
            case MANUAL -> ratePlan.getManualAmount();
        };
    }

    private record CalculationResult(Double masterBarAmount, Double finalAmount) {
    }

    private void validateProperty(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            throw new IllegalArgumentException("propertyId is required in path");
        }
        if (!propertyServiceClient.propertyExists(propertyId)) {
            throw new PropertyNotFoundException(propertyId);
        }
    }

    private void validateNoOverlapForActivePlan(String propertyId,
                                                java.util.Set<Long> roomTypeIds,
                                                String occupancyType,
                                                MealInclusion mealInclusion,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Long excludeRatePlanId) {
        long overlappingPlanCount = ratePlanRepository.countOverlappingActivePlans(
                propertyId,
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

    private void validateRoomTypeBelongsToProperty(String propertyId, Long roomTypeId) {
        if (roomTypeId == null) {
            throw new InvalidRatePlanException("Room type id is required");
        }
        validateRoomTypesBelongToProperty(propertyId, Set.of(roomTypeId));
    }

    private void validateRoomTypesBelongToProperty(String propertyId, Set<Long> roomTypeIds) {
        Set<Long> availableRoomTypeIds = fetchRoomTypeIdsByProperty(propertyId);
        Set<Long> invalidRoomTypeIds = roomTypeIds.stream()
                .filter(Objects::nonNull)
                .filter(roomTypeId -> !availableRoomTypeIds.contains(roomTypeId))
                .collect(Collectors.toSet());

        if (!invalidRoomTypeIds.isEmpty()) {
            throw new InvalidRatePlanException("Room type(s) do not belong to property: " + invalidRoomTypeIds);
        }
    }

    private Set<Long> fetchRoomTypeIdsByProperty(String propertyId) {
        RoomDTO[] roomTypes = roomServiceClient.getRoomTypesByProperty(propertyId);
        if (roomTypes == null || roomTypes.length == 0) {
            return Set.of();
        }

        return Arrays.stream(roomTypes)
                .map(RoomDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
