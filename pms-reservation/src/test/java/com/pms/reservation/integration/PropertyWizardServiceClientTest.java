package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import java.math.BigDecimal;
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
