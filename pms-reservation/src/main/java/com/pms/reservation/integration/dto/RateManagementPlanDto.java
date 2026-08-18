package com.pms.reservation.integration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RateManagementPlanDto {

    private Long id;
    private String propertyId;

    @JsonAlias({"ratePlanName", "planName", "plan", "ratePlan"})
    private String name;

    @JsonAlias({"rateCode", "planCode", "ratePlanCode"})
    private String code;

    @JsonAlias({"occupancy", "occupancyLabel"})
    private String occupancyType;

    @JsonAlias({"mealPlan", "mealPlanName", "meal"})
    private String mealOption;

    private String inclusion;
    private String type;
    private String status;
    private String startDate;
    private String endDate;

    private Long roomTypeId;
    @JsonAlias({"roomTypeName", "roomType"})
    private String roomType;

    private List<Long> applicableRoomTypeIds;

    private String calculationMethod;
    private BigDecimal adjustmentValue;
    private BigDecimal manualAmount;
    private Map<String, Object> manualPricingByOccupancy;
    private Long parentRatePlanId;

    private Map<String, Object> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void captureAdditionalField(String key, Object value) {
        additionalFields.put(key, value);
    }
}
