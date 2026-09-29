package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.GuestServiceProperties;
import com.pms.reservation.integration.dto.GuestProfileCreateRequest;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GuestServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GuestServiceProperties properties;

    private GuestServiceClient client;

    @BeforeEach
    void setUp() {
        client = new GuestServiceClient(restTemplate, properties);
        when(properties.getBaseUrl()).thenReturn("http://guest-service");
    }

    @Test
    void findExistingGuestReturnsResponseFromLookupEndpoint() {
        GuestLookupRequest request = GuestLookupRequest.builder()
                .propertyId("property-1")
                .phoneNumber("5551234")
                .build();
        GuestProfileResponse expected = GuestProfileResponse.builder()
                .id(42L)
                .guestId("GST-42")
                .propertyId("property-1")
                .build();
        when(restTemplate.exchange(
                eq("http://guest-service/api/v1/guests/lookup"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GuestProfileResponse.class)
        )).thenReturn(ResponseEntity.ok(expected));

        var result = client.findExistingGuest(request);

        assertThat(result).containsSame(expected);
    }

    @Test
    void findExistingGuestTreatsNotFoundAsNoMatch() {
        GuestLookupRequest request = GuestLookupRequest.builder()
                .propertyId("property-1")
                .phoneNumber("5551234")
                .build();
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GuestProfileResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThat(client.findExistingGuest(request)).isEmpty();
    }

    @Test
    void findExistingGuestDoesNotTreatServiceUnavailableAsNoMatch() {
        GuestLookupRequest request = GuestLookupRequest.builder()
                .propertyId("property-1")
                .phoneNumber("5551234")
                .build();
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GuestProfileResponse.class)
        )).thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> client.findExistingGuest(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Guest service request failed");
    }

    @Test
    void createGuestPostsProfileAndReturnsCreatedResponse() {
        GuestProfileCreateRequest request = GuestProfileCreateRequest.builder()
                .propertyId("property-1")
                .firstName("Ava")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 2))
                .idDocumentPath("uploads/guest-id.png")
                .build();
        GuestProfileResponse expected = GuestProfileResponse.builder()
                .id(42L)
                .guestId("GST-42")
                .propertyId("property-1")
                .firstName("Ava")
                .lastName("Guest")
                .build();
        when(restTemplate.exchange(
                eq("http://guest-service/api/v1/guests"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GuestProfileResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(expected));

        GuestProfileResponse response = client.createGuest(request);

        assertThat(response).isSameAs(expected);
        org.mockito.ArgumentCaptor<HttpEntity> requestEntity =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                eq("http://guest-service/api/v1/guests"),
                eq(HttpMethod.POST),
                requestEntity.capture(),
                eq(GuestProfileResponse.class)
        );
        assertThat(requestEntity.getValue().getBody()).isSameAs(request);
    }

    @Test
    void createGuestWrapsClientErrors() {
        GuestProfileCreateRequest request = GuestProfileCreateRequest.builder()
                .propertyId("property-1")
                .firstName("Ava")
                .lastName("Guest")
                .build();
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GuestProfileResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid guest profile"));

        assertThatThrownBy(() -> client.createGuest(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("HTTP 400");
    }
}
