package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

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
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
                .thenReturn(List.of(inventory));
        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
                .thenReturn(List.of(quote));

        RoomAvailabilityPricingDto availabilityItem = RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("5000.00"))
            .build();

        ReservationAvailabilityResponseDto mappedResponse = ReservationAvailabilityResponseDto.builder()
            .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
            .arrivalDate(LocalDate.of(2026, 7, 1))
            .departureDate(LocalDate.of(2026, 7, 3))
            .night(2)
            .numberOfRooms(1)
            .adults(2)
            .children(1)
            .groupCode("GRP001")
            .company("Contoso")
            .rateCode("BAR001")
            .availableRateCodes(List.of("BAR001"))
            .blockCode("BLK01")
            .availability(List.of(availabilityItem))
            .build();

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("5000.00"))
            .build());
        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenReturn(mappedResponse);

        var response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getPropertyId()).isEqualTo("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(response.getAvailability().get(0).getRatePlan()).isEqualTo("BAR");
        assertThat(response.getAvailability().get(0).getAvailableRooms()).isEqualTo(4);
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("5000.00");
        verify(reservationAvailabilityMapper).toResponse(eq(request), anyList(), anyList(), eq(List.of("BAR001")));
    }

        @Test
        void getAvailabilityShouldApplyPropertyWizardTaxRules() {
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
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("5000.00"));

        PropertyTaxRuleResponseDto taxRule = new PropertyTaxRuleResponseDto();
        taxRule.setRoomType("Deluxe King");
        taxRule.setTaxPercentage(new BigDecimal("18"));
        taxRule.setActive(true);

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of(taxRule));
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));
        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("5000.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
            .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getBaseRate()).isEqualByComparingTo("5000.00");
        assertThat(response.getAvailability().get(0).getTaxAmount()).isEqualByComparingTo("900.00");
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("5900.00");
        assertThat(response.getAvailableRateCodes()).containsExactly("BAR001");
        }

    @Test
    void getAvailabilityShouldRejectWhenPropertyWizardDisabled() {
        ReservationAvailabilityRequestDto request = validRequest();
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> reservationAvailabilityService.getAvailability(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Live inventory is unavailable because Property Wizard integration is disabled");

        verify(propertyInventoryPort, never()).fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull());
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

        @Test
        void getAvailabilityShouldFallbackWhenRequestedRateCodeDoesNotMatch() {
        ReservationAvailabilityRequestDto request = validRequest();
        request.setRateCode("BAR");

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomType("Deluxe King");
        quote.setRatePlan("STANDARD");
        quote.setRateCode("STANDARD");
        quote.setMealPlan("BB");
        quote.setBaseRate(new BigDecimal("5000.00"));
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("5000.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));
        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("STANDARD")
            .rateCode("STANDARD")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("5000.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
            .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("STANDARD");
        assertThat(response.getAvailableRateCodes()).containsExactly("STANDARD");
        }

    @Test
    void getAvailabilityShouldRetryRateQuotesPerRoomTypeWhenNullRoomTypeCallFails() {
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
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("5000.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenThrow(new ExternalServiceException("missing roomType"));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("5000.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
            .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("BAR001");
        assertThat(response.getAvailableRateCodes()).containsExactly("BAR001");
    }

    @Test
    void getAvailabilityShouldRetryRateQuotesPerRoomTypeWhenDirectFetchIsEmpty() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setRoomTypeId(101L);
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomType("Deluxe King");
        quote.setRatePlan("DLX Plan");
        quote.setRateCode("DLX");
        quote.setMealPlan("BB");
        quote.setBaseRate(new BigDecimal("4500.00"));
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("4500.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of());

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"), eq(101L), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("DLX Plan")
            .rateCode("DLX")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("4500.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("4500.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("DLX");
        assertThat(response.getAvailableRateCodes()).containsExactly("DLX");
    }

    @Test
    void getAvailabilityShouldRetryRateQuotesPerRoomTypeWhenDirectFetchIsNotJoinable() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("DLX");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto directQuote = new RatePlanPricingQuoteDto();
        directQuote.setRoomTypeId(28L);
        directQuote.setRoomType(null);
        directQuote.setRatePlan("DLX Plan");
        directQuote.setRateCode("DLX");
        directQuote.setMealPlan("BB");
        directQuote.setBaseRate(new BigDecimal("4500.00"));
        directQuote.setTaxAmount(BigDecimal.ZERO);
        directQuote.setFinalAmount(new BigDecimal("4500.00"));

        RatePlanPricingQuoteDto enrichedQuote = new RatePlanPricingQuoteDto();
        enrichedQuote.setRoomType("DLX");
        enrichedQuote.setRatePlan("DLX Plan");
        enrichedQuote.setRateCode("DLX");
        enrichedQuote.setMealPlan("BB");
        enrichedQuote.setBaseRate(new BigDecimal("4500.00"));
        enrichedQuote.setTaxAmount(BigDecimal.ZERO);
        enrichedQuote.setFinalAmount(new BigDecimal("4500.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(directQuote));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("DLX"), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(enrichedQuote));

        when(reservationAvailabilityMapper.toRoomAvailability(enrichedQuote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("DLX")
            .ratePlan("DLX Plan")
            .rateCode("DLX")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("4500.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("4500.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRoomType()).isEqualTo("DLX");
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("DLX");
        assertThat(response.getAvailableRateCodes()).containsExactly("DLX");
    }

    @Test
    void getAvailabilityShouldApplyPlanOnlyToApplicableRoomTypeAfterInventoryIdEnrichment() {
        ReservationAvailabilityRequestDto request = validRequest();
        request.setNumberOfRooms(1);
        request.setRateCode(null);

        PropertyRoomInventoryDto dlxInventory = new PropertyRoomInventoryDto();
        dlxInventory.setRoomType("DLX");
        dlxInventory.setAvailableRooms(20);

        PropertyRoomInventoryDto kngInventory = new PropertyRoomInventoryDto();
        kngInventory.setRoomType("KNG");
        kngInventory.setAvailableRooms(20);

        PropertyRoomOutletTypeDto dlxOutlet = new PropertyRoomOutletTypeDto();
        dlxOutlet.setId(27L);
        dlxOutlet.setRoomCode("DLX");
        dlxOutlet.setRoomName("Deluxe");

        PropertyRoomOutletTypeDto kngOutlet = new PropertyRoomOutletTypeDto();
        kngOutlet.setId(28L);
        kngOutlet.setRoomCode("KNG");
        kngOutlet.setRoomName("King");

        RatePlanPricingQuoteDto directQuote = new RatePlanPricingQuoteDto();
        directQuote.setRoomType(null);
        directQuote.setRoomTypeId(null);
        directQuote.setRatePlan("DeluzeRP");
        directQuote.setRateCode("DLX");
        directQuote.setMealPlan("breakfast and lunch");
        directQuote.setBaseRate(new BigDecimal("1800.00"));
        directQuote.setTaxAmount(BigDecimal.ZERO);
        directQuote.setFinalAmount(new BigDecimal("1800.00"));

        RatePlanPricingQuoteDto dlxPerRoomQuote = new RatePlanPricingQuoteDto();
        dlxPerRoomQuote.setRoomType("DLX");
        dlxPerRoomQuote.setRoomTypeId(27L);
        dlxPerRoomQuote.setRatePlan("DeluzeRP");
        dlxPerRoomQuote.setRateCode("DLX");
        dlxPerRoomQuote.setMealPlan("breakfast and lunch");
        dlxPerRoomQuote.setBaseRate(new BigDecimal("1800.00"));
        dlxPerRoomQuote.setTaxAmount(BigDecimal.ZERO);
        dlxPerRoomQuote.setFinalAmount(new BigDecimal("1800.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(dlxInventory, kngInventory));
        when(propertyInventoryPort.fetchRoomOutletTypes(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")))
            .thenReturn(List.of(dlxOutlet, kngOutlet));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(directQuote));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("DLX"), eq(27L), eq(2), eq(1)))
            .thenReturn(List.of(dlxPerRoomQuote));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("KNG"), eq(28L), eq(2), eq(1)))
            .thenReturn(List.of());

        when(reservationAvailabilityMapper.toRoomAvailability(dlxPerRoomQuote, dlxInventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("DLX")
            .ratePlan("DeluzeRP")
            .rateCode("DLX")
            .occupancy("2")
            .mealPlan("breakfast and lunch")
            .availableRooms(20)
            .baseRate(new BigDecimal("1800.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("1800.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRoomType()).isEqualTo("DLX");
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("DLX");
        assertThat(response.getAvailableRateCodes()).containsExactly("DLX");
    }

    @Test
    void getAvailabilityShouldJoinRateQuotesByRoomTypeIdWhenRoomTypeLabelMissing() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomTypeId(101L);
        inventory.setRoomType("Deluxe King");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
        quote.setRoomTypeId(101L);
        quote.setRoomType(null);
        quote.setRatePlan("Corporate Plan");
        quote.setRateCode("CORP");
        quote.setMealPlan("BB");
        quote.setBaseRate(new BigDecimal("4200.00"));
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("4200.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("Corporate Plan")
            .rateCode("CORP")
            .occupancy("2")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("4200.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("4200.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("CORP");
        assertThat(response.getAvailableRateCodes()).containsExactly("CORP");
    }

    @Test
    void getAvailabilityShouldKeepHigherAmountWhenDirectFetchReturnsDuplicateSignatures() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto zeroQuote = new RatePlanPricingQuoteDto();
        zeroQuote.setRoomTypeId(101L);
        zeroQuote.setRoomType("Deluxe King");
        zeroQuote.setRatePlan("Bar Rate");
        zeroQuote.setRateCode("BARR");
        zeroQuote.setOccupancy("2");
        zeroQuote.setMealPlan("breakfast");
        zeroQuote.setBaseRate(BigDecimal.ZERO);
        zeroQuote.setTaxAmount(BigDecimal.ZERO);
        zeroQuote.setFinalAmount(BigDecimal.ZERO);

        RatePlanPricingQuoteDto pricedQuote = new RatePlanPricingQuoteDto();
        pricedQuote.setRoomTypeId(999L);
        pricedQuote.setRoomType("Deluxe King");
        pricedQuote.setRatePlan("Bar Rate");
        pricedQuote.setRateCode("BARR");
        pricedQuote.setOccupancy("2");
        pricedQuote.setMealPlan("breakfast");
        pricedQuote.setBaseRate(new BigDecimal("1800.00"));
        pricedQuote.setTaxAmount(BigDecimal.ZERO);
        pricedQuote.setFinalAmount(new BigDecimal("1800.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(zeroQuote, pricedQuote));

        when(reservationAvailabilityMapper.toRoomAvailability(pricedQuote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("Bar Rate")
            .rateCode("BARR")
            .occupancy("2")
            .mealPlan("breakfast")
            .availableRooms(4)
            .baseRate(new BigDecimal("1800.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("1800.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("BARR");
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.getAvailableRateCodes()).containsExactly("BARR");
    }

    @Test
    void getAvailabilityShouldKeepHigherAmountWhenPerRoomFallbackReturnsDuplicateSignatures() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomTypeId(101L);
        inventory.setRoomType("Deluxe King");
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        RatePlanPricingQuoteDto zeroQuote = new RatePlanPricingQuoteDto();
        zeroQuote.setRoomTypeId(101L);
        zeroQuote.setRoomType("Deluxe King");
        zeroQuote.setRatePlan("Bar Rate");
        zeroQuote.setRateCode("BARR");
        zeroQuote.setOccupancy("2");
        zeroQuote.setMealPlan("breakfast");
        zeroQuote.setBaseRate(BigDecimal.ZERO);
        zeroQuote.setTaxAmount(BigDecimal.ZERO);
        zeroQuote.setFinalAmount(BigDecimal.ZERO);

        RatePlanPricingQuoteDto pricedQuote = new RatePlanPricingQuoteDto();
        pricedQuote.setRoomTypeId(999L);
        pricedQuote.setRoomType("Deluxe King");
        pricedQuote.setRatePlan("Bar Rate");
        pricedQuote.setRateCode("BARR");
        pricedQuote.setOccupancy("2");
        pricedQuote.setMealPlan("breakfast");
        pricedQuote.setBaseRate(new BigDecimal("1800.00"));
        pricedQuote.setTaxAmount(BigDecimal.ZERO);
        pricedQuote.setFinalAmount(new BigDecimal("1800.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of());

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"), eq(101L), eq(2), eq(1)))
            .thenReturn(List.of(zeroQuote, pricedQuote));

        when(reservationAvailabilityMapper.toRoomAvailability(pricedQuote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("Bar Rate")
            .rateCode("BARR")
            .occupancy("2")
            .mealPlan("breakfast")
            .availableRooms(4)
            .baseRate(new BigDecimal("1800.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("1800.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getRateCode()).isEqualTo("BARR");
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.getAvailableRateCodes()).containsExactly("BARR");
    }

        @Test
        void getAvailabilityShouldContinueWhenTaxRulesServiceFails() {
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
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setFinalAmount(new BigDecimal("5000.00"));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")))
            .thenThrow(new ExternalServiceException("Failed to fetch tax rules from Property Wizard service"));
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));
        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenReturn(List.of(quote));

        when(reservationAvailabilityMapper.toRoomAvailability(quote, inventory)).thenReturn(RoomAvailabilityPricingDto.builder()
            .roomType("Deluxe King")
            .ratePlan("BAR")
            .rateCode("BAR001")
            .occupancy("2 Adults")
            .mealPlan("BB")
            .availableRooms(4)
            .baseRate(new BigDecimal("5000.00"))
            .taxAmount(BigDecimal.ZERO)
            .finalAmount(new BigDecimal("5000.00"))
            .build());

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
            .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getPropertyId()).isEqualTo("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        assertThat(response.getAvailability()).hasSize(1);
        assertThat(response.getAvailability().get(0).getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getAvailability().get(0).getFinalAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getAvailableRateCodes()).containsExactly("BAR001");
        }

    @Test
    void getAvailabilityShouldSkipPerRoomRetryWhenRateManagementUnauthorized() {
        ReservationAvailabilityRequestDto request = validRequest();

        PropertyRoomInventoryDto inventory = new PropertyRoomInventoryDto();
        inventory.setRoomType("Deluxe King");
        inventory.setRoomTypeId(101L);
        inventory.setOccupancy("2 Adults");
        inventory.setAvailableRooms(4);

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))).thenReturn(List.of());
        when(propertyInventoryPort.fetchLiveInventory(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull()))
            .thenReturn(List.of(inventory));

        ExternalServiceException unauthorized = new ExternalServiceException(
            "Failed to fetch pricing from Rate Management service",
            HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            )
        );

        when(rateManagementPort.fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), isNull(), isNull(), eq(2), eq(1)))
            .thenThrow(unauthorized);

        when(reservationAvailabilityMapper.toResponse(eq(request), anyList(), anyList(), anyList())).thenAnswer(invocation ->
            ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .availability(invocation.getArgument(1))
                .next15DaysPricing(invocation.getArgument(2))
                .availableRateCodes(invocation.getArgument(3))
                .build()
        );

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertThat(response.getAvailability()).isEmpty();
        assertThat(response.getAvailableRateCodes()).isEmpty();
        verify(rateManagementPort, never()).fetchRateQuotes(
            eq("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 3)), eq("Deluxe King"), eq(101L), eq(2), eq(1));
    }

    private ReservationAvailabilityRequestDto validRequest() {
        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        request.setArrivalDate(LocalDate.of(2026, 7, 1));
        request.setDepartureDate(LocalDate.of(2026, 7, 3));
        request.setNight(2);
        request.setNumberOfRooms(1);
        request.setGroupCode("GRP001");
        request.setCompany("Contoso");
        request.setRateCode("BAR001");
        request.setBlockCode("BLK01");
        request.setRoomType("Deluxe King");
        request.setAdultCount(2);
        request.setChildCount(1);
        request.setAgeOfChild1(5);
        return request;
    }
}
