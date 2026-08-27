package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PropertyWizardServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PropertyWizardServiceProperties properties;

    private PropertyWizardServiceClient client;

    @BeforeEach
    void setUp() {
        client = new PropertyWizardServiceClient(restTemplate, properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
        void validateInventoryShouldForwardAuthorizationHeaderAndParseWrappedData() {
        withAuthorizationHeader("Bearer validate-token");
        when(properties.getBaseUrl()).thenReturn("http://localhost:8082");
        when(properties.getInventoryValidationPath()).thenReturn("/api/v1/property-wizard/inventory/validate");

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(ResponseEntity.ok(
            "{\"success\":true,\"data\":{\"propertyExists\":true,\"roomTypeAvailable\":true,\"availableRooms\":5},\"message\":\"ok\"}"
        ));

        PropertyInventoryValidationResponse validation = client.validateInventory("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", "Deluxe King", 2);

        assertThat(validation.getPropertyExists()).isTrue();
        assertThat(validation.getRoomTypeAvailable()).isTrue();
        assertThat(validation.getAvailableRooms()).isEqualTo(5);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)
        );

        String authorization = entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization).isEqualTo("Bearer validate-token");
        }

        @Test
        void fetchLiveInventoryShouldForwardAuthorizationHeaderAndParseWrappedData() {
        withAuthorizationHeader("Bearer test-token");
        when(properties.getBaseUrl()).thenReturn("http://localhost:8082");
        when(properties.getInventoryAvailabilityPath()).thenReturn("/api/rooms/properties/{propertyId}/inventory-rooms");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(ResponseEntity.ok(
            "{\"success\":true,\"data\":[{\"roomType\":\"Deluxe King\",\"occupancy\":\"2 Adults\",\"availableRooms\":4}],\"message\":\"ok\"}"
        ));

        var inventory = client.fetchLiveInventory(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 16),
                null
        );

        assertThat(inventory).hasSize(1);
        assertThat(inventory.get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(inventory.get(0).getAvailableRooms()).isEqualTo(4);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
            eq(String.class)
        );

        String authorization = entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization).isEqualTo("Bearer test-token");
    }

    @Test
    void fetchLiveInventoryShouldAggregateRoomLevelPayloadByRoomType() {
        when(properties.getBaseUrl()).thenReturn("http://localhost:8082");
        when(properties.getInventoryAvailabilityPath()).thenReturn("/api/rooms/properties/{propertyId}/inventory-rooms");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
            "{\"success\":true,\"data\":["
                + "{\"id\":141,\"roomTypeName\":\"DLX\",\"roomNumber\":\"101\"},"
                + "{\"id\":142,\"roomTypeName\":\"DLX\",\"roomNumber\":\"102\"},"
                + "{\"id\":151,\"roomTypeName\":\"KNG\",\"roomNumber\":\"111\"}"
                + "],\"message\":\"ok\"}"
        ));

        var inventory = client.fetchLiveInventory(
                "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 16),
                null
        );

        assertThat(inventory).hasSize(2);
        assertThat(inventory)
            .extracting(PropertyRoomInventoryDto::getRoomType)
            .containsExactlyInAnyOrder("DLX", "KNG");

        PropertyRoomInventoryDto dlxInventory = inventory.stream()
            .filter(item -> "DLX".equals(item.getRoomType()))
            .findFirst()
            .orElseThrow();
        PropertyRoomInventoryDto kngInventory = inventory.stream()
            .filter(item -> "KNG".equals(item.getRoomType()))
            .findFirst()
            .orElseThrow();

        assertThat(dlxInventory.getAvailableRooms()).isEqualTo(2);
        assertThat(kngInventory.getAvailableRooms()).isEqualTo(1);
    }

    @Test
        void fetchTaxRulesShouldForwardAuthorizationHeaderAndParseWrappedData() {
        withAuthorizationHeader("Bearer tax-token");
        when(properties.getBaseUrl()).thenReturn("http://localhost:8082");
        when(properties.getTaxRulesPath()).thenReturn("/api/taxes/properties/{propertyId}/rules");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(ResponseEntity.ok(
            "{\"success\":true,\"data\":[{\"roomType\":\"Deluxe King\",\"taxPercentage\":18,\"active\":true}],\"message\":\"ok\"}"
        ));

        var taxRules = client.fetchTaxRules("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");

        assertThat(taxRules).hasSize(1);
        assertThat(taxRules.get(0).getRoomType()).isEqualTo("Deluxe King");
        assertThat(taxRules.get(0).getTaxPercentage()).isEqualByComparingTo(new BigDecimal("18"));
        assertThat(taxRules.get(0).getActive()).isTrue();

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
            eq(String.class)
        );

        String authorization = entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization).isEqualTo("Bearer tax-token");
    }

    @Test
    void fetchRoomOutletTypesShouldForwardAuthorizationHeaderAndParseWrappedData() {
        withAuthorizationHeader("Bearer room-types-token");
        when(properties.getBaseUrl()).thenReturn("http://localhost:8082");
        when(properties.getRoomOutletTypesPath()).thenReturn("/api/rooms/properties/{propertyId}/room-outlet-types");

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(ResponseEntity.ok(
            "{\"success\":true,\"data\":[{\"id\":28,\"roomCode\":\"KNG\",\"roomName\":\"King\"}],\"message\":\"ok\"}"
        ));

        var outletTypes = client.fetchRoomOutletTypes("7cfd4559-b6f3-4b7d-b933-e93018ac1d47");

        assertThat(outletTypes).hasSize(1);
        PropertyRoomOutletTypeDto item = outletTypes.get(0);
        assertThat(item.getId()).isEqualTo(28L);
        assertThat(item.getRoomCode()).isEqualTo("KNG");
        assertThat(item.getRoomName()).isEqualTo("King");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            eq(String.class)
        );

        String authorization = entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization).isEqualTo("Bearer room-types-token");
    }

    private void withAuthorizationHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, value);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
