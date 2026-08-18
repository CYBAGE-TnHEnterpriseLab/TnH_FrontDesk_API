package com.pms.reservation.integration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatePlanAvailabilityResponseDto {

    private String id;

    @JsonAlias({"ratePlanName", "planName", "plan", "name"})
    private String ratePlan;

    @JsonAlias({"rateCode", "code", "planCode", "ratePlanCode"})
    private String rateCode;

    private Long roomTypeId;

    @JsonAlias({"applicableRoomTypeIds"})
    private List<Long> applicableRoomTypeIds;

    @JsonAlias({"roomTypeName"})
    private String roomType;

    @JsonAlias({"mealOption", "mealPlanName", "meal"})
    private String mealPlan;

    @JsonAlias({"occupancy", "occupancyLabel"})
    private String occupancyType;

    @JsonAlias({"price", "amount", "baseRate"})
    private BigDecimal basePrice;

    private String calculationMethod;

    private BigDecimal adjustmentValue;

    private BigDecimal manualAmount;

    private Map<String, Object> manualPricingByOccupancy;

    private Long parentRatePlanId;

    @JsonAlias({"isActive"})
    private Boolean active;

    private String inclusion;

    private Map<String, Object> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void captureAdditionalField(String key, Object value) {
        additionalFields.put(key, value);
    }
}
