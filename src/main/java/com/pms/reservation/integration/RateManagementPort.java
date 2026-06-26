package com.pms.reservation.integration;

import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.time.LocalDate;
import java.util.List;

public interface RateManagementPort {

    List<RatePlanPricingQuoteDto> fetchRateQuotes(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType,
            Integer adultCount,
            Integer childCount
    );
}
