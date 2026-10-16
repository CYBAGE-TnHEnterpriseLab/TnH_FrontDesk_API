package com.pms.reservation.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pms.reservation.config.AvailabilityPerformanceProperties;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.InventoryAvailabilityDto;
import com.pms.reservation.integration.dto.RatePlanPricingQuoteDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import com.pms.reservation.support.AvailabilityParallelExecutor;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationAvailabilityServiceImplTest {

    @Mock
    private PropertyInventoryPort propertyInventoryPort;

    @Mock
    private RateManagementPort rateManagementPort;

    @Mock
    private PropertyWizardServiceProperties propertyWizardServiceProperties;

    @Mock
    private ReservationAvailabilityMapper reservationAvailabilityMapper;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    private ReservationAvailabilityServiceImpl reservationAvailabilityService;

        @BeforeEach
        void setUp() {
                AvailabilityPerformanceProperties performanceProperties = new AvailabilityPerformanceProperties();
                performanceProperties.setParallelEnabled(false);
                reservationAvailabilityService = new ReservationAvailabilityServiceImpl(
                                propertyInventoryPort,
                                rateManagementPort,
                                propertyWizardServiceProperties,
                                reservationAvailabilityMapper,
                                inventoryServiceClient,
                                new AvailabilityParallelExecutor(performanceProperties),
                                performanceProperties
                );
        }

    @Test
    void getAvailabilityShouldReuseRoomOutletTypesAndPriceOnlyChildrenAboveFive() {
        String propertyId = "property-1";
        LocalDate arrivalDate = LocalDate.of(2026, 9, 20);
        PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
        roomType.setId(10L);
        roomType.setRoomCode("DLX");
        roomType.setRoomName("Deluxe");

        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId(propertyId);
        request.setArrivalDate(arrivalDate);
        request.setDepartureDate(arrivalDate.plusDays(3));
        request.setNight(3);
        request.setNumberOfRooms(1);
        request.setAdultCount(2);
        request.setChildCount(4);
        request.setChildAges(List.of(4, 5, 6, 8));

        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules(propertyId)).thenReturn(List.of());
        when(propertyInventoryPort.fetchRoomOutletTypes(propertyId)).thenReturn(List.of(roomType));
        when(inventoryServiceClient.availability(anyString(), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(rateManagementPort.fetchRateQuotes(
                eq(propertyId),
                any(LocalDate.class),
                any(LocalDate.class),
                any(),
                any(),
                eq(2),
                eq(2)
        )).thenReturn(List.of());
        when(reservationAvailabilityMapper.toResponse(any(), any(), any(), any()))
                .thenReturn(ReservationAvailabilityResponseDto.builder().build());

        reservationAvailabilityService.getAvailability(request);

        verify(propertyInventoryPort, times(1)).fetchRoomOutletTypes(propertyId);
        verify(propertyInventoryPort, times(1)).fetchTaxRules(propertyId);
        verify(inventoryServiceClient, times(1)).availability(
                eq(propertyId), anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void getAvailabilityShouldRejectNonpositiveAdultsBeforeCallingIntegrations(int adults) {
        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setAdultCount(adults);
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> reservationAvailabilityService.getAvailability(request));
        assertEquals("enter Adult should be greater then 0", exception.getMessage());
        verifyNoInteractions(propertyInventoryPort, rateManagementPort, inventoryServiceClient);
    }

    @ParameterizedTest
    @CsvSource({"1,3", "2,2", "3,1", "4,1", "5,1"})
    void getAvailabilityShouldPriceEachEligibleRoomAtItsMinimum(int adults, int expectedRooms) {
        AvailabilityPerformanceProperties performanceProperties = new AvailabilityPerformanceProperties();
        performanceProperties.setParallelEnabled(false);
        performanceProperties.setForecastDays(2);
        reservationAvailabilityService = new ReservationAvailabilityServiceImpl(
            propertyInventoryPort, rateManagementPort, propertyWizardServiceProperties,
            new ReservationAvailabilityMapper(), inventoryServiceClient,
            new AvailabilityParallelExecutor(performanceProperties), performanceProperties);

        LocalDate arrival = LocalDate.of(2026, 9, 20);
        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId("property-1");
        request.setArrivalDate(arrival);
        request.setDepartureDate(arrival.plusDays(1));
        request.setNight(1);
        request.setNumberOfRooms(1);
        request.setAdultCount(adults);

        List<PropertyRoomOutletTypeDto> roomTypes = new ArrayList<>();
        for (int minimum = 1; minimum <= 3; minimum++) {
            PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
            roomType.setId((long) minimum);
            roomType.setRoomCode("ROOM" + minimum);
            roomType.setRoomName("Room " + minimum);
            roomType.setMinimumGuestOccupancy(minimum);
            roomType.setMaximumGuestOccupancy(minimum == 3 ? 5 : minimum);
            roomTypes.add(roomType);
        }
        when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
        when(propertyInventoryPort.fetchTaxRules("property-1")).thenReturn(List.of());
        when(propertyInventoryPort.fetchRoomOutletTypes("property-1")).thenReturn(roomTypes);
        when(inventoryServiceClient.availability(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(
                new InventoryAvailabilityDto("property-1", null, arrival, 5, 0, 0, 5),
                new InventoryAvailabilityDto("property-1", null, arrival.plusDays(1), 5, 0, 0, 5)));
        when(rateManagementPort.fetchRateQuotes(eq("property-1"), any(), any(),
                any(), any(), anyInt(), eq(0)))
            .thenAnswer(invocation -> {
                RatePlanPricingQuoteDto quote = new RatePlanPricingQuoteDto();
                quote.setRoomType(invocation.getArgument(3));
                quote.setRoomTypeId(invocation.getArgument(4));
                int pricingAdults = invocation.getArgument(5);
                quote.setOccupancy(pricingAdults + " Adults");
                quote.setRatePlan("BAR");
                quote.setRateCode("BAR");
                quote.setBaseRate(BigDecimal.valueOf(pricingAdults * 100L));
                return List.of(quote);
            });

        ReservationAvailabilityResponseDto response = reservationAvailabilityService.getAvailability(request);

        assertEquals(adults, response.getAdults());
        assertThat(response.getAvailability()).hasSize(expectedRooms);
        assertThat(response.getRatePlans()).hasSize(1);
        assertThat(response.getRatePlans().getFirst().getRoomTypes()).hasSize(expectedRooms);
        assertThat(response.getNext15DaysPricing()).hasSize(2);
        response.getNext15DaysPricing().forEach(day ->
            assertThat(day.getAvailability()).hasSize(expectedRooms));
        for (PropertyRoomOutletTypeDto roomType : roomTypes) {
            // Only filter out rooms where guest count exceeds maximum occupancy
            boolean eligible = roomType.getMaximumGuestOccupancy() == null 
                || adults <= roomType.getMaximumGuestOccupancy();
            int pricingAdults = Math.max(adults, roomType.getMinimumGuestOccupancy());
            verify(rateManagementPort, times(eligible ? 2 : 0)).fetchRateQuotes(
                eq("property-1"), any(), any(), eq(roomType.getRoomName()), eq(roomType.getId()),
                eq(pricingAdults), eq(0));
            if (eligible) {
                response.getNext15DaysPricing().forEach(day -> {
                    var room = day.getAvailability().stream()
                        .filter(item -> roomType.getRoomName().equals(item.getRoomType()))
                        .findFirst().orElseThrow();
                    assertEquals(String.valueOf(pricingAdults), room.getOccupancy());
                    assertEquals(BigDecimal.valueOf(pricingAdults * 100L), room.getFinalAmount());
                });
            }
        }
    }

        @Test
        void getAvailabilityShouldRejectAdultsAboveMaximumRoomOccupancy() {
                String propertyId = "property-1";
                PropertyRoomOutletTypeDto roomType = new PropertyRoomOutletTypeDto();
                roomType.setId(10L);
                roomType.setMaximumGuestOccupancy(5);

                ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
                request.setPropertyId(propertyId);
                request.setArrivalDate(LocalDate.of(2026, 9, 20));
                request.setDepartureDate(LocalDate.of(2026, 9, 21));
                request.setAdultCount(6);

                when(propertyWizardServiceProperties.isEnabled()).thenReturn(true);
                when(propertyInventoryPort.fetchTaxRules(propertyId)).thenReturn(List.of());
                when(propertyInventoryPort.fetchRoomOutletTypes(propertyId)).thenReturn(List.of(roomType));

                BadRequestException exception = assertThrows(
                                BadRequestException.class,
                                () -> reservationAvailabilityService.getAvailability(request));

                assertEquals("The maximum occupancy for this property is 5", exception.getMessage());
                verify(inventoryServiceClient, times(0)).availability(
                                anyString(), anyString(), any(LocalDate.class), any(LocalDate.class));
        }
}
