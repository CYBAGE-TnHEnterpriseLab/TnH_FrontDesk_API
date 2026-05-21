package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanPriceResponseDTO;
import com.frontdesk.pms.rate_management.dto.RatePlanResponseDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.RatePlan;
import com.frontdesk.pms.rate_management.enums.MealInclusion;
import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.enums.RatePlanType;
import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.RatePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatePlanServiceTest {

    @Mock
    private RatePlanRepository ratePlanRepository;

    @Mock
    private MasterRoomPricingRepository masterRoomPricingRepository;

    @InjectMocks
    private RatePlanService ratePlanService;

    @Test
    void createRatePlan_shouldFailWhenCodeAlreadyExists() {
        RatePlanRequestDTO requestDTO = validRequest();
        when(ratePlanRepository.existsByCodeIgnoreCase("BAR10")).thenReturn(true);

        assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(requestDTO));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    @Test
    void createRatePlan_shouldFailWhenDateRangeInvalid() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setStartDate(LocalDate.of(2026, 6, 10));
        requestDTO.setEndDate(LocalDate.of(2026, 6, 1));

        assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(requestDTO));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
    }

    @Test
    void getAvailableRatePlans_shouldReturnActivePlansForRoomTypeAndDate() {
        RatePlan plan = new RatePlan();
        plan.setId(1L);
        plan.setName("Corporate 10");
        plan.setCode("CORP10");
        plan.setOccupancyType("2P");
        plan.setMealInclusion(MealInclusion.BREAKFAST_INCLUDED);
        plan.setType(RatePlanType.REFUNDABLE);
        plan.setStatus(RatePlanStatus.ACTIVE);
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 30));
        plan.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        plan.setAdjustmentValue(10.0);
        plan.setApplicableRoomTypeIds(Set.of(101L, 102L));

        when(ratePlanRepository.findAvailableByRoomTypeOccupancyMealAndDate(
            101L,
            "2P",
            MealInclusion.BREAKFAST_INCLUDED,
            LocalDate.of(2026, 6, 10),
            RatePlanStatus.ACTIVE))
                .thenReturn(List.of(plan));

        List<RatePlanResponseDTO> result = ratePlanService.getAvailableRatePlans(
            101L,
            "2P",
            MealInclusion.BREAKFAST_INCLUDED,
            LocalDate.of(2026, 6, 10));

        assertEquals(1, result.size());
        assertEquals("CORP10", result.get(0).getCode());
    }

    @Test
    void calculatePriceFromMasterBar_shouldUseMasterBarForPercentOff() {
        RatePlan plan = new RatePlan();
        plan.setId(10L);
        plan.setOccupancyType("2P");
        plan.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        plan.setAdjustmentValue(10.0);
        plan.setApplicableRoomTypeIds(Set.of(101L));

        MasterRoomPricing pricing = new MasterRoomPricing();
        pricing.setRoomTypeId(101L);
        pricing.setOccupancyType("2P");
        pricing.setPrice(2000.0);

        when(ratePlanRepository.findById(anyLong())).thenReturn(java.util.Optional.of(plan));
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(101L, "2P"))
                .thenReturn(Optional.of(pricing));

        RatePlanPriceResponseDTO priceResponseDTO = ratePlanService.calculatePriceFromMasterBar(10L, 101L);
        assertEquals(2000.0, priceResponseDTO.getMasterBarAmount());
        assertEquals(1800.0, priceResponseDTO.getFinalAmount());
    }

    @Test
    void createRatePlan_shouldFailWhenMealInclusionMissing() {
        RatePlanRequestDTO requestDTO = validRequest();
        requestDTO.setMealInclusion(null);

        InvalidRatePlanException exception =
                assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(requestDTO));
        assertTrue(exception.getMessage().contains("Meal inclusion"));
    }

        @Test
        void createRatePlan_shouldFailWhenOverlappingActivePlanExists() {
        RatePlanRequestDTO requestDTO = validRequest();
        when(ratePlanRepository.existsByCodeIgnoreCase("BAR10")).thenReturn(false);
        when(ratePlanRepository.countOverlappingActivePlans(
            requestDTO.getApplicableRoomTypeIds(),
            requestDTO.getOccupancyType(),
            requestDTO.getMealInclusion(),
            requestDTO.getStartDate(),
            requestDTO.getEndDate(),
            RatePlanStatus.ACTIVE,
            null)).thenReturn(1L);

        InvalidRatePlanException exception =
            assertThrows(InvalidRatePlanException.class, () -> ratePlanService.createRatePlan(requestDTO));

        assertTrue(exception.getMessage().contains("Overlapping active rate plan"));
        verify(ratePlanRepository, never()).save(org.mockito.ArgumentMatchers.any(RatePlan.class));
        }

        @Test
        void updateRatePlanStatus_shouldFailWhenActivatingOverlappingPlan() {
        RatePlan existing = new RatePlan();
        existing.setId(22L);
        existing.setStatus(RatePlanStatus.INACTIVE);
        existing.setOccupancyType("2P");
        existing.setMealInclusion(MealInclusion.BREAKFAST_INCLUDED);
        existing.setApplicableRoomTypeIds(Set.of(101L));
        existing.setStartDate(LocalDate.of(2026, 6, 1));
        existing.setEndDate(LocalDate.of(2026, 6, 30));

        when(ratePlanRepository.findById(22L)).thenReturn(Optional.of(existing));
        when(ratePlanRepository.countOverlappingActivePlans(
            existing.getApplicableRoomTypeIds(),
            existing.getOccupancyType(),
            existing.getMealInclusion(),
            existing.getStartDate(),
            existing.getEndDate(),
            RatePlanStatus.ACTIVE,
            existing.getId())).thenReturn(1L);

        InvalidRatePlanException exception = assertThrows(
            InvalidRatePlanException.class,
            () -> ratePlanService.updateRatePlanStatus(22L, RatePlanStatus.ACTIVE));

        assertTrue(exception.getMessage().contains("Overlapping active rate plan"));
        verify(ratePlanRepository, never()).save(existing);
        }

    private RatePlanRequestDTO validRequest() {
        RatePlanRequestDTO requestDTO = new RatePlanRequestDTO();
        requestDTO.setName("BAR 10 Off");
        requestDTO.setCode("BAR10");
        requestDTO.setOccupancyType("2P");
        requestDTO.setMealInclusion(MealInclusion.BREAKFAST_INCLUDED);
        requestDTO.setType(RatePlanType.REFUNDABLE);
        requestDTO.setStartDate(LocalDate.of(2026, 6, 1));
        requestDTO.setEndDate(LocalDate.of(2026, 6, 30));
        requestDTO.setApplicableRoomTypeIds(Set.of(101L));
        requestDTO.setCalculationMethod(RatePlanCalculationMethod.PERCENT_OFF_BAR);
        requestDTO.setAdjustmentValue(10.0);
        return requestDTO;
    }
}
