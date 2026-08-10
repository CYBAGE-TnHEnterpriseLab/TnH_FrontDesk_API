package Policy_Management.Policy.service.impl;

import Policy_Management.Policy.dto.APIResponse;
import Policy_Management.Policy.dto.PropertyDto;
import Policy_Management.Policy.exception.PolicyValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PropertyClient propertyClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(propertyClient, "propertyServiceUrl", "http://property/api/{propertyId}");
    }

    @Test
    void getPropertyById_returnsProperty_whenApiResponds() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        PropertyDto propertyDto = new PropertyDto();
        propertyDto.setId("P-1");
        propertyDto.setPropertyCode("CODE-1");
        APIResponse<PropertyDto> body = new APIResponse<>("success", "ok", propertyDto);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class),
                eq("P-1")))
                .thenReturn(ResponseEntity.ok(body));

        PropertyDto response = propertyClient.getPropertyById("P-1");

        assertEquals("P-1", response.getId());
        assertEquals("CODE-1", response.getPropertyCode());
    }

    @Test
    void getPropertyById_throwsValidation_whenBodyDataIsNull() {
        when(request.getHeader("Authorization")).thenReturn(null);

        APIResponse<PropertyDto> body = new APIResponse<>("success", "ok", null);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class),
                eq("P-2")))
                .thenReturn(ResponseEntity.ok(body));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class,
                () -> propertyClient.getPropertyById("P-2"));

        assertNotNull(ex.getErrors());
        assertTrue(ex.getErrors().get("propertyId").contains("lookup failed"));
    }

    @Test
    void getPropertyById_throwsValidation_whenHttpErrorOccurs() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class),
                eq("P-3")))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found",
                        HttpHeaders.EMPTY, new byte[0], null));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class,
                () -> propertyClient.getPropertyById("P-3"));

        assertNotNull(ex.getErrors());
        assertTrue(ex.getErrors().get("propertyId").contains("status 404"));
    }

    @Test
    void getPropertyById_throwsValidation_whenUnexpectedErrorOccurs() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class),
                eq("P-4")))
                .thenThrow(new RuntimeException("downstream unavailable"));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class,
                () -> propertyClient.getPropertyById("P-4"));

        assertNotNull(ex.getErrors());
        assertTrue(ex.getErrors().get("propertyId").contains("downstream unavailable"));
    }
}
