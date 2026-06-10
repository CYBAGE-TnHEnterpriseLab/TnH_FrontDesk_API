package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.enums.RatePlanType;
import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import com.frontdesk.pms.rate_management.exception.RatePlanNotFoundException;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.RatePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RatePlanServiceTest {

    private static final String PROPERTY_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private RatePlanRepository ratePlanRepository;

    @Mock
    private MasterRoomPricingRepository masterRoomPricingRepository;

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @Mock
    private RoomServiceClient roomServiceClient;

    @InjectMocks
    private RatePlanService ratePlanService;

    @Test
    void createRatePlan_shouldFailWhenCodeAlreadyExists() {
        RatePlanRequestDTO requestDTO = validRequest();
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));
        when(ratePlanRepository.existsByPropertyIdAndCodeIgnoreCase(PROPERTY_ID, "BAR10")).thenReturn(true);

        assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(PROPERTY_ID, requestDTO));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    @Test
    void createRatePlan_shouldFailWhenDateRangeInvalid() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setStartDate(LocalDate.of(2026, 6, 10));
        requestDTO.setEndDate(LocalDate.of(2026, 6, 1));
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);

        assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(PROPERTY_ID, requestDTO));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    @Test
    void getAllRatePlans_shouldReturnNewestFirst() {
        RatePlan newest = new RatePlan();
        newest.setId(2L);
        newest.setPropertyId(PROPERTY_ID);
        newest.setName("New Plan");
        newest.setCode("NEW");
        newest.setOccupancyType("2 Guest");
        newest.setMealOption(MasterRoomMealOption.BREAKFAST);
        newest.setType(RatePlanType.REFUNDABLE);
        newest.setStatus(RatePlanStatus.ACTIVE);
        newest.setStartDate(LocalDate.of(2026, 6, 1));
        newest.setEndDate(LocalDate.of(2026, 6, 30));

        RatePlan older = new RatePlan();
        older.setId(1L);
        older.setPropertyId(PROPERTY_ID);
        older.setName("Old Plan");
        older.setCode("OLD");
        older.setOccupancyType("2 Guest");
        older.setMealOption(MasterRoomMealOption.BREAKFAST);
        older.setType(RatePlanType.REFUNDABLE);
        older.setStatus(RatePlanStatus.ACTIVE);
        older.setStartDate(LocalDate.of(2026, 6, 1));
        older.setEndDate(LocalDate.of(2026, 6, 30));

        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(ratePlanRepository.findByPropertyIdOrderByIdDesc(PROPERTY_ID)).thenReturn(List.of(newest, older));

        List<RatePlanResponseDTO> result = ratePlanService.getAllRatePlans(PROPERTY_ID);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    void getAvailableRatePlans_shouldReturnActivePlansForRoomTypeAndDate() {
        RatePlan plan = new RatePlan();
        plan.setId(1L);
        plan.setName("Corporate 10");
        plan.setCode("CORP10");
        plan.setOccupancyType("2 Guest");
        plan.setMealOption(MasterRoomMealOption.BREAKFAST);
        plan.setInclusion("Wifi");
        plan.setType(RatePlanType.REFUNDABLE);
        plan.setStatus(RatePlanStatus.ACTIVE);
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 30));
        plan.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        plan.setAdjustmentValue(10.0);
        plan.setApplicableRoomTypeIds(Set.of(101L, 102L));

        when(ratePlanRepository.findAvailableByRoomTypeMealAndDate(
            PROPERTY_ID,
            101L,
            MasterRoomMealOption.BREAKFAST,
            LocalDate.of(2026, 6, 10),
            RatePlanStatus.ACTIVE))
                .thenReturn(List.of(plan));
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));

        List<RatePlanResponseDTO> result = ratePlanService.getAvailableRatePlans(
            PROPERTY_ID,
            101L,
            "2 Guest",
            MasterRoomMealOption.BREAKFAST,
            LocalDate.of(2026, 6, 10));

        assertEquals(1, result.size());
        assertEquals("CORP10", result.get(0).getCode());
    }

    @Test
    void calculatePriceFromMasterBar_shouldUseMasterBarForPercentOff() {
        RatePlan plan = new RatePlan();
        plan.setId(10L);
        plan.setOccupancyType("2 Guest");
        plan.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        plan.setAdjustmentValue(10.0);
        plan.setApplicableRoomTypeIds(Set.of(101L));

        MasterRoomPricing pricing = new MasterRoomPricing();
        pricing.setRoomTypeId(101L);
        pricing.setOccupancyType("2 Guest");
        pricing.setPrice(2000.0);

        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(101L, "2 Guest"))
                .thenReturn(Optional.of(pricing));
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));
        when(ratePlanRepository.findByIdAndPropertyId(10L, PROPERTY_ID)).thenReturn(Optional.of(plan));

        RatePlanPriceResponseDTO priceResponseDTO = ratePlanService.calculatePriceFromMasterBar(PROPERTY_ID, 10L, 101L);
        assertEquals(2000.0, priceResponseDTO.getMasterBarAmount());
        assertEquals(1800.0, priceResponseDTO.getFinalAmount());
    }

    @Test
    void calculatePriceFromMasterBar_shouldDeriveFromParentRatePlan() {
        RatePlan barPlan = new RatePlan();
        barPlan.setId(10L);
        barPlan.setOccupancyType("2 Guest");
        barPlan.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        barPlan.setAdjustmentValue(10.0);
        barPlan.setApplicableRoomTypeIds(Set.of(101L));

        RatePlan childPlan = new RatePlan();
        childPlan.setId(11L);
        childPlan.setOccupancyType("2 Guest");
        childPlan.setCalculationMethod(RatePlanCalculationMethod.FLAT_OFF_BAR);
        childPlan.setAdjustmentValue(100.0);
        childPlan.setParentRatePlanId(10L);
        childPlan.setApplicableRoomTypeIds(Set.of(101L));

        MasterRoomPricing pricing = new MasterRoomPricing();
        pricing.setRoomTypeId(101L);
        pricing.setOccupancyType("2 Guest");
        pricing.setPrice(2000.0);

        when(ratePlanRepository.findByIdAndPropertyId(11L, PROPERTY_ID)).thenReturn(Optional.of(childPlan));
        when(ratePlanRepository.findByIdAndPropertyId(10L, PROPERTY_ID)).thenReturn(Optional.of(barPlan));
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(101L, "2 Guest"))
                .thenReturn(Optional.of(pricing));
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));

        RatePlanPriceResponseDTO priceResponseDTO = ratePlanService.calculatePriceFromMasterBar(PROPERTY_ID, 11L, 101L);

        assertEquals(2000.0, priceResponseDTO.getMasterBarAmount());
        assertEquals(1700.0, priceResponseDTO.getFinalAmount());
    }

    @Test
    void createRatePlan_shouldFailWhenMealOptionMissing() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setMealOption(null);
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);

        InvalidRatePlanException exception =
            assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(PROPERTY_ID, requestDTO));
        assertTrue(exception.getMessage().contains("Meal option"));
    }

    @Test
    void createRatePlan_shouldFailWhenRoomTypeDoesNotBelongToProperty() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setApplicableRoomTypeIds(Set.of(999L));

        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));

        InvalidRatePlanException exception = assertThrows(
                InvalidRatePlanException.class,
                () -> ratePlanService.createRatePlan(PROPERTY_ID, requestDTO));

        assertTrue(exception.getMessage().contains("do not belong to property"));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    @Test
    void createRatePlan_shouldAllowManualPricingByOccupancyWithoutManualAmount() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setCalculationMethod(RatePlanCalculationMethod.MANUAL);
        requestDTO.setManualAmount(null);
        requestDTO.setManualPricingByOccupancy(Map.of("1 Guest", 1800.0, "2 Guest", 2200.0));

        RatePlan saved = new RatePlan();
        saved.setId(99L);
        saved.setPropertyId(PROPERTY_ID);
        saved.setName(requestDTO.getName());
        saved.setCode(requestDTO.getCode());
        saved.setOccupancyType(requestDTO.getOccupancyType());
        saved.setMealOption(requestDTO.getMealOption());
        saved.setInclusion(requestDTO.getInclusion());
        saved.setType(requestDTO.getType());
        saved.setStatus(RatePlanStatus.ACTIVE);
        saved.setStartDate(requestDTO.getStartDate());
        saved.setEndDate(requestDTO.getEndDate());
        saved.setCalculationMethod(RatePlanCalculationMethod.MANUAL);
        saved.setManualPricingByOccupancy(requestDTO.getManualPricingByOccupancy());
        saved.setApplicableRoomTypeIds(requestDTO.getApplicableRoomTypeIds());

        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));
        when(ratePlanRepository.existsByPropertyIdAndCodeIgnoreCase(PROPERTY_ID, requestDTO.getCode())).thenReturn(false);
        when(ratePlanRepository.countOverlappingActivePlans(
                PROPERTY_ID,
                requestDTO.getApplicableRoomTypeIds(),
                "1 Guest",
                requestDTO.getMealOption(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                RatePlanStatus.ACTIVE,
                null)).thenReturn(0L);
        when(ratePlanRepository.countOverlappingActivePlans(
                PROPERTY_ID,
                requestDTO.getApplicableRoomTypeIds(),
                "2 Guest",
                requestDTO.getMealOption(),
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                RatePlanStatus.ACTIVE,
                null)).thenReturn(0L);
        when(ratePlanRepository.save(org.mockito.ArgumentMatchers.any(RatePlan.class))).thenReturn(saved);

        RatePlanResponseDTO result = ratePlanService.createRatePlan(PROPERTY_ID, requestDTO);

        assertEquals(99L, result.getId());
        assertEquals(2200.0, result.getManualPricingByOccupancy().get("2 Guest"));
    }

        @Test
        void createRatePlan_shouldFailWhenOverlappingActivePlanExists() {
        RatePlanRequestDTO requestDTO = validRequest();
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(roomServiceClient.getRoomTypesByProperty(PROPERTY_ID)).thenReturn(roomTypes(101L, 102L));
        when(ratePlanRepository.existsByPropertyIdAndCodeIgnoreCase(PROPERTY_ID, "BAR10")).thenReturn(false);
        when(ratePlanRepository.countOverlappingActivePlans(
            PROPERTY_ID,
            requestDTO.getApplicableRoomTypeIds(),
            requestDTO.getOccupancyType(),
            requestDTO.getMealOption(),
            requestDTO.getStartDate(),
            requestDTO.getEndDate(),
            RatePlanStatus.ACTIVE,
            null)).thenReturn(1L);

        InvalidRatePlanException exception =
            assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(PROPERTY_ID, requestDTO));

        assertTrue(exception.getMessage().contains("Overlapping active rate plan"));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
        }

        @Test
        void updateRatePlanStatus_shouldFailWhenActivatingOverlappingPlan() {
        RatePlan existing = new RatePlan();
        existing.setId(22L);
        existing.setStatus(RatePlanStatus.INACTIVE);
        existing.setOccupancyType("2 Guest");
        existing.setMealOption(MasterRoomMealOption.BREAKFAST);
        existing.setInclusion("Wifi");
        existing.setApplicableRoomTypeIds(Set.of(101L));
        existing.setStartDate(LocalDate.of(2026, 6, 1));
        existing.setEndDate(LocalDate.of(2026, 6, 30));

        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(ratePlanRepository.findByIdAndPropertyId(22L, PROPERTY_ID)).thenReturn(Optional.of(existing));
        when(ratePlanRepository.countOverlappingActivePlans(
            PROPERTY_ID,
            existing.getApplicableRoomTypeIds(),
            existing.getOccupancyType(),
            existing.getMealOption(),
            existing.getStartDate(),
            existing.getEndDate(),
            RatePlanStatus.ACTIVE,
            existing.getId())).thenReturn(1L);

        InvalidRatePlanException exception = assertThrows(
            InvalidRatePlanException.class,
            () -> ratePlanService.updateRatePlanStatus(PROPERTY_ID, 22L, RatePlanStatus.ACTIVE));

        assertTrue(exception.getMessage().contains("Overlapping active rate plan"));
        verify(ratePlanRepository, never()).save(existing);
        }

    @Test
    void deleteRatePlan_shouldDeleteWhenExists() {
        RatePlan existing = new RatePlan();
        existing.setId(57L);

        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(ratePlanRepository.findByIdAndPropertyId(57L, PROPERTY_ID)).thenReturn(Optional.of(existing));

        ratePlanService.deleteRatePlan(PROPERTY_ID, 57L);

        verify(ratePlanRepository, times(1)).delete(existing);
    }

    @Test
    void deleteRatePlan_shouldFailWhenNotFound() {
        when(propertyServiceClient.propertyExists(PROPERTY_ID)).thenReturn(true);
        when(ratePlanRepository.findByIdAndPropertyId(58L, PROPERTY_ID)).thenReturn(Optional.empty());

        assertThrows(RatePlanNotFoundException.class, () -> ratePlanService.deleteRatePlan(PROPERTY_ID, 58L));
        verify(ratePlanRepository, never()).delete(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    private RatePlanRequestDTO validRequest() {
        RatePlanRequestDTO requestDTO = new RatePlanRequestDTO();
        requestDTO.setName("BAR 10 Off");
        requestDTO.setCode("BAR10");
        requestDTO.setOccupancyType("2 Guest");
        requestDTO.setMealOption(MasterRoomMealOption.BREAKFAST);
        requestDTO.setInclusion("Wifi, Laundry");
        requestDTO.setType(RatePlanType.REFUNDABLE);
        requestDTO.setStartDate(LocalDate.of(2026, 6, 1));
        requestDTO.setEndDate(LocalDate.of(2026, 6, 30));
        requestDTO.setApplicableRoomTypeIds(Set.of(101L));
        requestDTO.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        requestDTO.setAdjustmentValue(10.0);
        return requestDTO;
    }

    private RoomDTO[] roomTypes(Long... roomTypeIds) {
        RoomDTO[] roomTypes = new RoomDTO[roomTypeIds.length];
        for (int i = 0; i < roomTypeIds.length; i++) {
            RoomDTO roomType = new RoomDTO();
            roomType.setId(roomTypeIds[i]);
            roomType.setActive(true);
            roomTypes[i] = roomType;
        }
        return roomTypes;
    }
}

