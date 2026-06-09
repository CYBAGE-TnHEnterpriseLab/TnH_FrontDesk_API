package com.frontdesk.pms.rate_management.dto;

import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.enums.RatePlanType;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Data
public class RatePlanResponseDTO {
    private Long id;
    private String propertyId;
    private String name;
    private String code;
    private String occupancyType;
    private MasterRoomMealOption mealOption;
    private String inclusion;
    private RatePlanType type;
    private RatePlanStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<Long> applicableRoomTypeIds;
    private RatePlanCalculationMethod calculationMethod;
    private Double adjustmentValue;
    private Double manualAmount;
    private Map<String, Double> manualPricingByOccupancy;
    private Long parentRatePlanId;
}
