package com.frontdesk.pms.rate_management.dto;

import com.frontdesk.pms.rate_management.enums.RatePlanCalculationMethod;
import com.frontdesk.pms.rate_management.enums.MealInclusion;
import com.frontdesk.pms.rate_management.enums.RatePlanStatus;
import com.frontdesk.pms.rate_management.enums.RatePlanType;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class RatePlanResponseDTO {
    private Long id;
    private String name;
    private String code;
    private String occupancyType;
    private MealInclusion mealInclusion;
    private RatePlanType type;
    private RatePlanStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<Long> applicableRoomTypeIds;
    private RatePlanCalculationMethod calculationMethod;
    private Double adjustmentValue;
    private Double manualAmount;
}
