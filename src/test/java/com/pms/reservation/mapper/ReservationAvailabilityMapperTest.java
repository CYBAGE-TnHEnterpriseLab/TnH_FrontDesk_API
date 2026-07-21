package com.pms.reservation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationAvailabilityMapperTest {

    private final ReservationAvailabilityMapper mapper = new ReservationAvailabilityMapper();

    @Test
    void toRoomAvailabilityShouldNormalizeSingleToNumericCount() {
        RatePlanPricingQuoteDto quote = baseQuote();
        quote.setOccupancy("Single");

        PropertyRoomInventoryDto inventory = baseInventory();
        inventory.setOccupancy("2 Adults");

        RoomAvailabilityPricingDto response = mapper.toRoomAvailability(quote, inventory);

        assertThat(response.getOccupancy()).isEqualTo("1");
    }

    @Test
    void toRoomAvailabilityShouldNormalizeSimpleNumericOccupancy() {
        RatePlanPricingQuoteDto quote = baseQuote();
        quote.setOccupancy("1 Adults");

        PropertyRoomInventoryDto inventory = baseInventory();

        RoomAvailabilityPricingDto response = mapper.toRoomAvailability(quote, inventory);

        assertThat(response.getOccupancy()).isEqualTo("1");
    }

    @Test
    void toRoomAvailabilityShouldConvertComplexOccupancyTextToTotalCount() {
        RatePlanPricingQuoteDto quote = baseQuote();
        quote.setOccupancy("2 Adults + 1 Child");

        PropertyRoomInventoryDto inventory = baseInventory();

        RoomAvailabilityPricingDto response = mapper.toRoomAvailability(quote, inventory);

        assertThat(response.getOccupancy()).isEqualTo("3");
    }

    @Test
    void toResponseShouldGroupAvailabilityByRatePlanWithNestedRoomTypes() {
        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId("PROP001");
        request.setArrivalDate(LocalDate.of(2026, 7, 1));
        request.setDepartureDate(LocalDate.of(2026, 7, 3));
        request.setNight(2);
        request.setNumberOfRooms(1);
        request.setAdultCount(2);
        request.setChildCount(0);

        RoomAvailabilityPricingDto barDlx = RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(new BigDecimal("900.00"))
            .finalAmount(new BigDecimal("5900.00"))
            .build();

        RoomAvailabilityPricingDto barSuite = RoomAvailabilityPricingDto.builder()
            .roomType("Suite")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2")
            .mealPlan("BB")
            .availableRooms(2)
            .baseRate(new BigDecimal("7000.00"))
            .taxAmount(new BigDecimal("1260.00"))
            .finalAmount(new BigDecimal("8260.00"))
            .build();

        RoomAvailabilityPricingDto corpDlx = RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("Corporate")
            .rateCode("CORP")
            .occupancy("2")
            .mealPlan("RO")
            .availableRooms(4)
            .baseRate(new BigDecimal("4500.00"))
            .taxAmount(new BigDecimal("810.00"))
            .finalAmount(new BigDecimal("5310.00"))
            .build();

        ReservationAvailabilityResponseDto response = mapper.toResponse(
            request,
            List.of(barDlx, corpDlx, barSuite),
            List.of(),
            List.of("BAR001", "CORP")
        );

        assertThat(response.getRatePlans()).hasSize(2);
        assertThat(response.getRatePlans().get(0).getRatePlan()).isEqualTo("BAR");
        assertThat(response.getRatePlans().get(0).getRateCode()).isEqualTo("BAR001");
        assertThat(response.getRatePlans().get(0).getRoomTypes()).hasSize(2);
        assertThat(response.getRatePlans().get(0).getRoomTypes().get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(response.getRatePlans().get(0).getRoomTypes().get(1).getRoomType()).isEqualTo("Suite");

        assertThat(response.getRatePlans().get(1).getRatePlan()).isEqualTo("Corporate");
        assertThat(response.getRatePlans().get(1).getRateCode()).isEqualTo("CORP");
        assertThat(response.getRatePlans().get(1).getRoomTypes()).hasSize(1);
        assertThat(response.getRatePlans().get(1).getRoomTypes().get(0).getFinalAmount()).isEqualByComparingTo("5310.00");
    }

    private RatePlanPricingQuoteDto baseQuote() {
        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomType("Deluxe King");
        quote.setRatePlan("BAR");
        quote.setRateCode("BAR001");
        quote.setMealPlan("BB");
        quote.setBaseRate(new BigDecimal("5000.00"));
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("5000.00"));
        return quote;
    }

    private PropertyRoomInventoryDto baseInventory() {
        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setAvailableRooms(4);
        return inventory;
    }
}
