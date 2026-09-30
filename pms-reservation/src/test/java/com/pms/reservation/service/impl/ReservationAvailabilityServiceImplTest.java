package com.pms.reservation.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.reservation.config.AvailabilityPerformanceProperties;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.RateManagementPort;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.mapper.ReservationAvailabilityMapper;
import com.pms.reservation.support.AvailabilityParallelExecutor;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    void getAvailabilityShouldReuseRoomOutletTypesAcrossPrimaryAndNext15Days() {
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
        request.setChildCount(0);

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
                eq(0)
        )).thenReturn(List.of());
        when(reservationAvailabilityMapper.toResponse(any(), any(), any(), any()))
                .thenReturn(ReservationAvailabilityResponseDto.builder().build());

        reservationAvailabilityService.getAvailability(request);

        verify(propertyInventoryPort, times(1)).fetchRoomOutletTypes(propertyId);
        verify(propertyInventoryPort, times(1)).fetchTaxRules(propertyId);
        verify(inventoryServiceClient, times(1)).availability(
                eq(propertyId), anyString(), any(LocalDate.class), any(LocalDate.class));
    }
}
