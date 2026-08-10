package Policy_Management.Policy.controller;

import Policy_Management.Policy.dto.APIResponse;
import Policy_Management.Policy.dto.PolicyDto;
import Policy_Management.Policy.dto.PolicyListResponse;
import Policy_Management.Policy.dto.Status;
import Policy_Management.Policy.security.CurrentUserProvider;
import Policy_Management.Policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    private PolicyService service;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private PolicyController controller;

    @BeforeEach
    void setUp() {
        controller = new PolicyController(service, currentUserProvider);
    }

    @Test
    void create_setsCreatedBy_andReturnsCreated() {
        PolicyDto request = new PolicyDto();
        request.setPolicyCode("C-1");
        PolicyDto created = new PolicyDto();
        created.setId(10L);

        when(currentUserProvider.getCurrentUsername()).thenReturn("user-a");
        when(service.createPolicy(request)).thenReturn(created);

        ResponseEntity<APIResponse<PolicyDto>> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("user-a", request.getCreatedBy());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().getStatus());
        assertEquals(10L, response.getBody().getData().getId());
    }

    @Test
    void getAll_returnsOk() {
        PolicyListResponse listResponse = new PolicyListResponse(List.of());
        when(service.getAllPolicies(Status.ACTIVE)).thenReturn(listResponse);

        ResponseEntity<APIResponse<PolicyListResponse>> response = controller.getAll(Status.ACTIVE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(listResponse, response.getBody().getData());
    }

    @Test
    void getById_returnsOk() {
        PolicyDto dto = new PolicyDto();
        dto.setId(1L);
        when(service.getPolicyById(1L)).thenReturn(dto);

        ResponseEntity<APIResponse<PolicyDto>> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void getByPropertyId_returnsOk() {
        PolicyListResponse listResponse = new PolicyListResponse(List.of());
        when(service.getAllPoliciesByPropertyId("P-1")).thenReturn(listResponse);

        ResponseEntity<APIResponse<PolicyListResponse>> response = controller.getByPropertyId("P-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(listResponse, response.getBody().getData());
    }

    @Test
    void update_returnsOk() {
        PolicyDto request = new PolicyDto();
        PolicyDto updated = new PolicyDto();
        updated.setPolicyCode("NEW");
        when(service.updatePolicy(5L, request)).thenReturn(updated);

        ResponseEntity<APIResponse<PolicyDto>> response = controller.update(5L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("NEW", response.getBody().getData().getPolicyCode());
    }

    @Test
    void mapPolicyToProperty_returnsOk() {
        PolicyDto mapped = new PolicyDto();
        mapped.setPropertyId("P-9");
        when(service.mapPolicyToProperty(2L, "P-9")).thenReturn(mapped);

        ResponseEntity<APIResponse<PolicyDto>> response = controller.mapPolicyToProperty(2L, "P-9");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("P-9", response.getBody().getData().getPropertyId());
    }

    @Test
    void unmapPolicyFromProperty_returnsOk() {
        PolicyDto unmapped = new PolicyDto();
        unmapped.setPropertyId(null);
        when(service.unmapPolicyFromProperty(3L)).thenReturn(unmapped);

        ResponseEntity<APIResponse<PolicyDto>> response = controller.unmapPolicyFromProperty(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().getData().getPropertyId());
    }

    @Test
    void delete_returnsOk() {
        ResponseEntity<APIResponse<Void>> response = controller.delete(6L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).deletePolicy(6L);
    }
}
