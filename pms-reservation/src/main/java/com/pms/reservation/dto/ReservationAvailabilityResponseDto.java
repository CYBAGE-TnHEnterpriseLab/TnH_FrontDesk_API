package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationAvailabilityResponseDto {
    String propertyId;
    LocalDate arrivalDate;
    LocalDate departureDate;
    Integer night;
    Integer numberOfRooms;
    Integer adults;
    Integer children;
    Integer ageOfChild1;
    Integer ageOfChild2;
    String groupCode;
    String company;
    String rateCode;
    List<String> availableRateCodes;
    List<RatePlanAvailabilityDto> ratePlans;
    String blockCode;
    @JsonIgnore
    List<RoomAvailabilityPricingDto> availability;
    List<DailyAvailabilityPricingDto> next15DaysPricing;
}
