package com.pms.reservation.mapper;

import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationAvailabilityMapper {

    public RoomAvailabilityPricingDto toRoomAvailability(
            RatePlanPricingQuoteDto quote,
            PropertyRoomInventoryDto inventory
    ) {
        String occupancy = StringUtils.hasText(quote.getOccupancy())
                ? quote.getOccupancy()
                : inventory.getOccupancy();

        return RoomAvailabilityPricingDto.builder()
                .roomType(quote.getRoomType())
                .ratePlan(quote.getRatePlan())
                .rateCode(quote.getRateCode())
                .occupancy(occupancy)
                .mealPlan(quote.getMealPlan())
                .availableRooms(inventory.getAvailableRooms())
                .baseRate(quote.getBaseRate())
                .taxAmount(quote.getTaxAmount())
                .finalAmount(quote.getFinalAmount())
                .build();
    }

    public ReservationAvailabilityResponseDto toResponse(
            ReservationAvailabilityRequestDto request,
            List<RoomAvailabilityPricingDto> availability
    ) {
        return ReservationAvailabilityResponseDto.builder()
                .propertyId(request.getPropertyId())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .selectedRoomType(request.getRoomType())
                .availability(availability)
                .build();
    }
}
