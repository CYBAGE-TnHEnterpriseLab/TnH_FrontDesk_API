package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.OccupancyType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatePlanService {

    private final RatePlanRepository ratePlanRepository;
    private final MasterRoomPricingRepository masterRoomPricingRepository;
    private final PropertyWizardClient propertyWizardClient;

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
            extractSupportedOccupancies(requestDTO),
            requestDTO.getMealOption(),
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
        existing.setOccupancyType(normalizeOccupancyType(requestDTO.getOccupancyType()));
        existing.setMealOption(requestDTO.getMealOption());
        existing.setInclusion(requestDTO.getInclusion());
        existing.setType(requestDTO.getType());
        existing.setStartDate(requestDTO.getStartDate());
        existing.setEndDate(requestDTO.getEndDate());
        existing.setCalculationMethod(requestDTO.getCalculationMethod());
        existing.setAdjustmentValue(requestDTO.getAdjustmentValue());
        existing.setManualAmount(requestDTO.getManualAmount());
        existing.setManualPricingByOccupancy(normalizeManualPricingByOccupancy(requestDTO.getManualPricingByOccupancy()));
        existing.setParentRatePlanId(requestDTO.getParentRatePlanId());
        existing.setApplicableRoomTypeIds(new HashSet<>(requestDTO.getApplicableRoomTypeIds()));

        if (existing.getStatus() == RatePlanStatus.ACTIVE) {
            validateNoOverlapForActivePlan(
                    propertyId,
                    existing.getApplicableRoomTypeIds(),
                    extractSupportedOccupancies(existing),
                    existing.getMealOption(),
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
        return ratePlanRepository.findByPropertyIdOrderByIdDesc(propertyId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRatePlan(String propertyId, Long ratePlanId) {
        validateProperty(propertyId);
        RatePlan ratePlan = ratePlanRepository.findByIdAndPropertyId(ratePlanId, propertyId)
                .orElseThrow(() -> new RatePlanNotFoundException(ratePlanId));
        ratePlanRepository.delete(ratePlan);
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
                    extractSupportedOccupancies(ratePlan),
                    ratePlan.getMealOption(),
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
                                                            MasterRoomMealOption mealOption,
                                                            LocalDate stayDate) {
        validateProperty(propertyId);
        validateRoomTypeBelongsToProperty(propertyId, roomTypeId);
        String normalizedOccupancyType = normalizeOccupancyType(occupancyType);
        return ratePlanRepository.findAvailableByRoomTypeMealAndDate(
                        propertyId,
                        roomTypeId,
                        mealOption,
                        stayDate,
                        RatePlanStatus.ACTIVE)
                .stream()
            .filter(ratePlan -> supportsOccupancy(ratePlan, normalizedOccupancyType))
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
        normalizeOccupancyType(requestDTO.getOccupancyType());
        if (requestDTO.getType() == null) {
            throw new InvalidRatePlanException("Rate plan type is required");
        }
        if (requestDTO.getMealOption() == null) {
            throw new InvalidRatePlanException("Meal option is required");
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

            Set<String> requestedOccupancies = extractSupportedOccupancies(requestDTO);
            Set<String> parentOccupancies = extractSupportedOccupancies(parentRatePlan);
            if (!parentOccupancies.containsAll(requestedOccupancies)) {
                throw new InvalidRatePlanException("Parent and child rate plans must have compatible occupancy types");
            }
        }

        if (requestDTO.getCalculationMethod() == RatePlanCalculationMethod.MANUAL) {
            validateManualPricing(requestDTO);
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
        entity.setOccupancyType(normalizeOccupancyType(dto.getOccupancyType()));
        entity.setMealOption(dto.getMealOption());
        entity.setInclusion(dto.getInclusion());
        entity.setType(dto.getType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCalculationMethod(dto.getCalculationMethod());
        entity.setAdjustmentValue(dto.getAdjustmentValue());
        entity.setManualAmount(dto.getManualAmount());
        entity.setManualPricingByOccupancy(normalizeManualPricingByOccupancy(dto.getManualPricingByOccupancy()));
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
        dto.setMealOption(entity.getMealOption());
        dto.setInclusion(entity.getInclusion());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setApplicableRoomTypeIds(entity.getApplicableRoomTypeIds());
        dto.setCalculationMethod(entity.getCalculationMethod());
        dto.setAdjustmentValue(entity.getAdjustmentValue());
        dto.setManualAmount(entity.getManualAmount());
        dto.setManualPricingByOccupancy(copyManualPricingByOccupancy(entity.getManualPricingByOccupancy()));
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
                case MANUAL -> new CalculationResult(null, Math.max(0.0, resolveManualAmount(ratePlan)));
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
            case MANUAL -> resolveManualAmount(ratePlan);
        };
    }

    private record CalculationResult(Double masterBarAmount, Double finalAmount) {
    }

    private void validateProperty(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            throw new IllegalArgumentException("propertyId is required in path");
        }
        if (!propertyWizardClient.propertyExists(propertyId)) {
            throw new PropertyNotFoundException(propertyId);
        }

        reconcileExistingRatePlansWithPropertyWizard(propertyId);
    }

    private void reconcileExistingRatePlansWithPropertyWizard(String propertyId) {
        Set<Long> availableRoomTypeIds = fetchRoomTypeIdsByProperty(propertyId);
        List<RatePlan> existingRatePlans = ratePlanRepository.findByPropertyId(propertyId);
        if (existingRatePlans == null || existingRatePlans.isEmpty()) {
            return;
        }

        List<RatePlan> ratePlansToUpdate = new ArrayList<>();
        for (RatePlan ratePlan : existingRatePlans) {
            Set<Long> applicableRoomTypeIds = ratePlan.getApplicableRoomTypeIds();
            if (applicableRoomTypeIds == null) {
                applicableRoomTypeIds = Set.of();
            }

            Set<Long> sanitizedRoomTypeIds = applicableRoomTypeIds.stream()
                    .filter(Objects::nonNull)
                    .filter(availableRoomTypeIds::contains)
                    .collect(Collectors.toCollection(HashSet::new));

            boolean roomTypesChanged = !sanitizedRoomTypeIds.equals(applicableRoomTypeIds);
            boolean shouldDeactivate = sanitizedRoomTypeIds.isEmpty() && ratePlan.getStatus() == RatePlanStatus.ACTIVE;

            if (roomTypesChanged || shouldDeactivate) {
                ratePlan.setApplicableRoomTypeIds(sanitizedRoomTypeIds);
                if (shouldDeactivate) {
                    ratePlan.setStatus(RatePlanStatus.INACTIVE);
                }
                ratePlansToUpdate.add(ratePlan);
            }
        }

        if (!ratePlansToUpdate.isEmpty()) {
            ratePlanRepository.saveAll(ratePlansToUpdate);
        }
    }

    private void validateNoOverlapForActivePlan(String propertyId,
                                                java.util.Set<Long> roomTypeIds,
                                                Set<String> occupancyTypes,
                                                MasterRoomMealOption mealOption,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Long excludeRatePlanId) {
        Set<String> normalizedOccupancies = occupancyTypes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        for (String occupancyType : normalizedOccupancies) {
            long overlappingPlanCount = ratePlanRepository.countOverlappingActivePlans(
                    propertyId,
                    roomTypeIds,
                    occupancyType,
                    mealOption,
                    startDate,
                    endDate,
                    RatePlanStatus.ACTIVE,
                    excludeRatePlanId);

            if (overlappingPlanCount > 0) {
                throw new InvalidRatePlanException(
                        "Overlapping active rate plan exists for same room type, occupancy, meal option, and date range");
            }
        }
    }

    private void validateManualPricing(RatePlanRequestDTO requestDTO) {
        Map<String, Double> manualPricingByOccupancy = requestDTO.getManualPricingByOccupancy();
        if (manualPricingByOccupancy == null || manualPricingByOccupancy.isEmpty()) {
            if (requestDTO.getManualAmount() == null || requestDTO.getManualAmount() < 0) {
                throw new InvalidRatePlanException("Manual amount must be provided and cannot be negative");
            }
            return;
        }

        for (Map.Entry<String, Double> entry : manualPricingByOccupancy.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new InvalidRatePlanException("Occupancy type is required for manual occupancy pricing");
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new InvalidRatePlanException("Manual amount cannot be negative for occupancy " + entry.getKey());
            }
        }
    }

    private Set<String> extractSupportedOccupancies(RatePlanRequestDTO requestDTO) {
        Set<String> occupancies = new HashSet<>();
        if (requestDTO.getOccupancyType() != null && !requestDTO.getOccupancyType().isBlank()) {
            occupancies.add(normalizeOccupancyType(requestDTO.getOccupancyType()));
        }
        if (requestDTO.getManualPricingByOccupancy() != null) {
            occupancies.addAll(requestDTO.getManualPricingByOccupancy().keySet().stream()
                    .filter(Objects::nonNull)
                .map(this::normalizeOccupancyType)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet()));
        }
        return occupancies;
    }

    private Set<String> extractSupportedOccupancies(RatePlan ratePlan) {
        Set<String> occupancies = new HashSet<>();
        if (ratePlan.getOccupancyType() != null && !ratePlan.getOccupancyType().isBlank()) {
            occupancies.add(normalizeOccupancyType(ratePlan.getOccupancyType()));
        }
        if (ratePlan.getManualPricingByOccupancy() != null) {
            occupancies.addAll(ratePlan.getManualPricingByOccupancy().keySet().stream()
                    .filter(Objects::nonNull)
                .map(this::normalizeOccupancyType)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet()));
        }
        return occupancies;
    }

    private boolean supportsOccupancy(RatePlan ratePlan, String occupancyType) {
        if (occupancyType == null || occupancyType.isBlank()) {
            return false;
        }
        return extractSupportedOccupancies(ratePlan).contains(normalizeOccupancyType(occupancyType));
    }

    private Double resolveManualAmount(RatePlan ratePlan) {
        if (ratePlan.getManualPricingByOccupancy() != null
                && ratePlan.getOccupancyType() != null
                && ratePlan.getManualPricingByOccupancy().containsKey(ratePlan.getOccupancyType())) {
            Double occupancyManualAmount = ratePlan.getManualPricingByOccupancy().get(ratePlan.getOccupancyType());
            if (occupancyManualAmount != null) {
                return occupancyManualAmount;
            }
        }

        if (ratePlan.getManualAmount() == null) {
            throw new InvalidRatePlanException("Manual amount not configured for rate plan");
        }
        return ratePlan.getManualAmount();
    }

    private Map<String, Double> copyManualPricingByOccupancy(Map<String, Double> manualPricingByOccupancy) {
        if (manualPricingByOccupancy == null || manualPricingByOccupancy.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(manualPricingByOccupancy);
    }

    private Map<String, Double> normalizeManualPricingByOccupancy(Map<String, Double> manualPricingByOccupancy) {
        Map<String, Double> normalized = new HashMap<>();
        if (manualPricingByOccupancy == null || manualPricingByOccupancy.isEmpty()) {
            return normalized;
        }

        for (Map.Entry<String, Double> entry : manualPricingByOccupancy.entrySet()) {
            normalized.put(normalizeOccupancyType(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private String normalizeOccupancyType(String occupancyType) {
        return OccupancyType.normalizeOrThrow(occupancyType);
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
        RoomDTO[] roomTypes = propertyWizardClient.getRoomTypesByProperty(propertyId);
        if (roomTypes == null || roomTypes.length == 0) {
            return Set.of();
        }

        return Arrays.stream(roomTypes)
                .map(RoomDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
