package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationAvailabilityServiceImplTest {

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Mock
    private RateManagementPort rateManagementPort;

    @Mock
    private ReservationAvailabilityMapper reservationAvailabilityMapper;

    @InjectMocks
    private ReservationAvailabilityServiceImpl reservationAvailabilityService;

    @Test
    void getAvailabilityShouldMergeInventoryAndRateQuotes() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomType("Deluxe King");
        quote.setRatePlan("BAR");
        quote.setRateCode("BAR001");
        quote.setMealPlan("BB");
        quote.setBaseRate(new BigDecimal("5000.00"));
        quote.setTaxAmount(new BigDecimal("900.00"));
        quote.setFinalAmount(new BigDecimal("5900.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchLiveInventory(
                eq("PROP001"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King")))
                .thenReturn(List.of(inventory));
        when(rateManagementPort.fetchRateQuotes(
                eq("PROP001"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"), eq(2), eq(1)))
                .thenReturn(List.of(quote));

        RoomAvailabilityPricingDto availabilityItem = RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(new BigDecimal("900.00"))
            .finalAmount(new BigDecimal("5900.00"))
            .build();

        ReservationAvailabilityResponseDto mappedResponse = ReservationAvailabilityResponseDto.builder()
            .propertyId("PROP001")
            .arrivalDate(LocalDate.of(2026, 7, 1))
            .departureDate(LocalDate.of(2026, 7, 3))
            .selectedRoomType("Deluxe King")
            .availability(List.of(availabilityItem))
            .build();

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(availabilityItem);
        when(reservationAvailabilityMapper.toResponse(eq(request), any())).thenReturn(mappedResponse);

        var response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getPropertyId()).isEqualTo("PROP001");
        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(response.getAvailability().get(0).getRatePlan()).isEqualTo("BAR");
        assertThat(response.getAvailability().get(0).getAvailableRooms()).isEqualTo(4);
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("5900.00");
    }

    @Test
    void getAvailabilityShouldRejectWhenPropertyWizardDisabled() {
        ReservationAvailabilityRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> reservationAvailabilityService.getAvailability(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Live inventory is unavailable because Property Wizard integration is disabled");

        verify(propertyInventoryPort, never()).fetchLiveInventory(
                eq("PROP001"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"));
        verifyNoInteractions(rateManagementPort);
    }

    @Test
    void getAvailabilityShouldRejectInvalidDateRange() {
        ReservationAvailabilityRequestDto request = validRequest();
        request.setArrivalDate(LocalDate.of(2026, 7, 5));
        request.setDepartureDate(LocalDate.of(2026, 7, 3));

        assertThatThrownBy(() -> reservationAvailabilityService.getAvailability(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("departureDate must be on or after arrivalDate");

        verifyNoInteractions(propertyInventoryPort);
        verifyNoInteractions(rateManagementPort);
    }

    private ReservationAvailabilityRequestDto validRequest() {
        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId("PROP001");
        request.setArrivalDate(LocalDate.of(2026, 7, 1));
        request.setDepartureDate(LocalDate.of(2026, 7, 3));
        request.setRoomType("Deluxe King");
        request.setAdultCount(2);
        request.setChildCount(1);
        return request;
    }
}
