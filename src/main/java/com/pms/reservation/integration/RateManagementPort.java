package com.pms.reservation.integration;

import com.pms.reservation.integration.dto.RateManagementPlanDto;
import com.pms.reservation.integration.dto.RatePlanCalculatedPriceResponseDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RateManagementPort {

    List<RatePlanPricingQuoteDto> fetchRateQuotes(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType,
            Long roomTypeId,
            Integer adultCount,
            Integer childCount
    );

    List<RateManagementPlanDto> listRatePlans(String propertyId);

    List<RateManagementPlanDto> getAvailableRatePlans(
            String propertyId,
            Long roomTypeId,
            String occupancyType,
            String mealOption,
            LocalDate stayDate
    );

    RatePlanCalculatedPriceResponseDto getCalculatedPrice(
            String propertyId,
            Long ratePlanId,
            Long roomTypeId
    );

    Map<Long, BigDecimal> getPricingByRoomTypeForRatePlan(
            String propertyId,
            Long ratePlanId
    );
}
