package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.reservation.config.InventoryServiceProperties;
import com.pms.reservation.integration.dto.InventoryAvailabilityDto;
import com.pms.reservation.integration.dto.InventoryReservationRequest;
import com.pms.reservation.integration.dto.InventoryReservationResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class InventoryServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InventoryServiceProperties properties;

    private InventoryServiceClient client;

    @BeforeEach
    void setUp() {
        client = new InventoryServiceClient(restTemplate, properties);
    }

    @Test
    void reserveUsesConfirmationNumberContract() {
                withAuthorizationHeader("Bearer inventory-token");
                when(properties.getBaseUrl()).thenReturn("http://inventory");
                when(properties.getReservationsPath()).thenReturn("/api/inventory/reservations");
        InventoryReservationRequest request = InventoryReservationRequest.builder()
                .confirmationNumber("CNF-100")
                .propertyId("property-1")
                .bookedRoomTypeId("room-type-1")
                .assignedRoomTypeId("room-type-1")
                .checkInDate(LocalDate.of(2026, 7, 1))
                .checkOutDate(LocalDate.of(2026, 7, 3))
                .quantity(1)
                .build();
        InventoryReservationResponse expected = new InventoryReservationResponse(
                "CNF-100", "property-1", "room-type-1", "room-type-1",
                request.getCheckInDate(), request.getCheckOutDate(), 1, "RESERVED", false);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(InventoryReservationResponse.class))).thenReturn(ResponseEntity.ok(expected));

        InventoryReservationResponse response = client.reserve(request);

        assertThat(response).isEqualTo(expected);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(url.capture(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(InventoryReservationResponse.class));
        assertThat(url.getValue()).contains("/api/inventory/reservations");
        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST), entity.capture(),
                eq(InventoryReservationResponse.class));
        assertThat(entity.getValue().getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer inventory-token");
    }

        private void withAuthorizationHeader(String value) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.addHeader("Authorization", value);
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        }

    @Test
    void availabilityReturnsInventoryRows() {
                when(properties.getBaseUrl()).thenReturn("http://inventory");
                when(properties.getAvailabilityPath()).thenReturn("/api/inventory/availability");
        InventoryAvailabilityDto row = new InventoryAvailabilityDto(
                "property-1", "room-type-1", LocalDate.of(2026, 7, 1),
                5, 1, 0, 4);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(InventoryAvailabilityDto[].class))).thenReturn(ResponseEntity.ok(new InventoryAvailabilityDto[]{row}));

        var result = client.availability("property-1", "room-type-1",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));

        assertThat(result).containsExactly(row);
    }
}
