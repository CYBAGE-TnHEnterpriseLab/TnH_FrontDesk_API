package Policy_Management.Policy.service.impl;

import Policy_Management.Policy.dto.PolicyDto;
import Policy_Management.Policy.dto.PolicyListResponse;
import Policy_Management.Policy.dto.PropertyDto;
import Policy_Management.Policy.dto.Status;
import Policy_Management.Policy.entity.Policy;
import Policy_Management.Policy.exception.DuplicatePolicyException;
import Policy_Management.Policy.exception.PolicyNotFoundException;
import Policy_Management.Policy.exception.PolicyValidationException;
import Policy_Management.Policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private PolicyRepository repository;

    @Mock
    private PropertyClient propertyClient;

    @InjectMocks
    private PolicyServiceImpl service;

    private PolicyDto baseDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "propertyClient", propertyClient);
        baseDto = new PolicyDto();
        baseDto.setPolicyName("Policy A");
        baseDto.setPolicyType("Type A");
        baseDto.setServiceType("Service A");
        baseDto.setUsedBy("Team A");
        baseDto.setPolicyCode("P-001");
        baseDto.setPolicyCategory("Category A");
        baseDto.setCreatedBy("tester");
    }

    @Test
    void createPolicy_setsDefaultStatus_andSaves_whenUnique() {
        when(repository.existsByPolicyCode("P-001")).thenReturn(false);
        when(repository.countByStatus(Status.DRAFT)).thenReturn(3);
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PolicyDto created = service.createPolicy(baseDto);

        assertEquals(1L, created.getId());
        assertEquals(Status.DRAFT, created.getStatus());
        assertEquals(4, created.getPolicyCount());
        verify(repository).updatePolicyCountByStatus(Status.DRAFT, 3);
    }

    @Test
    void createPolicy_enrichesProperty_whenPropertyIdProvided() {
        baseDto.setPropertyId("PROP-1");
        when(repository.existsByPolicyCode("P-001")).thenReturn(false);
        when(repository.countByStatus(Status.DRAFT)).thenReturn(0);
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PropertyDto property = new PropertyDto();
        property.setId("PROP-1");
        property.setPropertyCode("PCODE-1");
        when(propertyClient.getPropertyById("PROP-1")).thenReturn(property);

        PolicyDto created = service.createPolicy(baseDto);

        assertEquals("PCODE-1", created.getPropertyCode());
        verify(propertyClient).getPropertyById("PROP-1");
    }

    @Test
    void createPolicy_throwsDuplicatePolicyException_whenCodeExists() {
        when(repository.existsByPolicyCode("P-001")).thenReturn(true);

        assertThrows(DuplicatePolicyException.class, () -> service.createPolicy(baseDto));

        verify(repository, never()).save(any());
    }

    @Test
    void createPolicy_throwsValidation_forPublishedMissingFields() {
        PolicyDto published = new PolicyDto();
        published.setStatus(Status.PUBLISHED);

        PolicyValidationException ex = assertThrows(PolicyValidationException.class,
                () -> service.createPolicy(published));

        assertNotNull(ex.getErrors());
        assertTrue(ex.getErrors().containsKey("policyName"));
        assertTrue(ex.getErrors().containsKey("createdBy"));
    }

    @Test
    void createPolicy_publishedWithAllFields_succeeds() {
        PolicyDto published = new PolicyDto();
        published.setStatus(Status.PUBLISHED);
        published.setPolicyName("Published");
        published.setPolicyType("Type");
        published.setServiceType("Service");
        published.setUsedBy("Team");
        published.setPolicyCode("PUB-1");
        published.setPolicyCategory("Category");
        published.setCreatedBy("creator");

        when(repository.existsByPolicyCode("PUB-1")).thenReturn(false);
        when(repository.countByStatus(Status.PUBLISHED)).thenReturn(1);
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        PolicyDto created = service.createPolicy(published);

        assertEquals(99L, created.getId());
        assertEquals(Status.PUBLISHED, created.getStatus());
        verify(repository).updatePolicyCountByStatus(Status.PUBLISHED, 1);
    }

    @Test
    void getAllPolicies_withoutStatus_returnsAggregatedCounts() {
        Policy active = policy(1L, "A", Status.ACTIVE, "P1", "PC1");
        Policy draft = policy(2L, "B", Status.DRAFT, "P2", "PC2");

        when(repository.findAll()).thenReturn(List.of(active, draft));
        when(repository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(active));
        when(repository.findAllByStatus(Status.DRAFT)).thenReturn(List.of(draft));
        when(repository.findAllByStatus(Status.INACTIVE)).thenReturn(List.of());

        PolicyListResponse response = service.getAllPolicies(null);

        assertEquals(2, response.getPolicies().size());
        assertEquals(2, response.getTotalPolicies());
        assertEquals(1, response.getActivePolicies());
        assertEquals(1, response.getDraftPolicies());
        assertEquals(0, response.getInactivePolicies());
    }

    @Test
    void getAllPolicies_withStatus_usesFilterRepositoryCall() {
        Policy active = policy(1L, "A", Status.ACTIVE, null, null);
        when(repository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of(active));
        when(repository.findAll()).thenReturn(List.of(active));
        when(repository.findAllByStatus(Status.DRAFT)).thenReturn(List.of());
        when(repository.findAllByStatus(Status.INACTIVE)).thenReturn(List.of());

        PolicyListResponse response = service.getAllPolicies(Status.ACTIVE);

        assertEquals(1, response.getPolicies().size());
        verify(repository, times(2)).findAllByStatus(Status.ACTIVE);
    }

    @Test
    void getPolicyById_returnsPolicy_whenFound() {
        Policy found = policy(1L, "A", Status.ACTIVE, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(found));

        PolicyDto response = service.getPolicyById(1L);

        assertEquals(1L, response.getId());
        assertEquals("A", response.getPolicyName());
    }

    @Test
    void getPolicyById_throws_whenNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> service.getPolicyById(9L));
    }

    @Test
    void updatePolicy_throws_whenNotFound() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> service.updatePolicy(7L, new PolicyDto()));
    }

    @Test
    void updatePolicy_updatesStatusAndPolicyCount_whenStatusChanged() {
        Policy existing = policy(10L, "A", Status.DRAFT, null, null);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.countByStatus(Status.DRAFT)).thenReturn(4);
        when(repository.countByStatus(Status.ACTIVE)).thenReturn(6);

        PolicyDto update = new PolicyDto();
        update.setStatus(Status.ACTIVE);

        PolicyDto result = service.updatePolicy(10L, update);

        assertEquals(Status.ACTIVE, result.getStatus());
        verify(repository).updatePolicyCountByStatus(Status.DRAFT, 4);
        verify(repository).updatePolicyCountByStatus(Status.ACTIVE, 6);
    }

    @Test
    void updatePolicy_doesNotUpdateCount_whenStatusUnchanged() {
        Policy existing = policy(11L, "A", Status.ACTIVE, null, null);
        when(repository.findById(11L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyDto update = new PolicyDto();

        PolicyDto result = service.updatePolicy(11L, update);

        assertEquals(Status.ACTIVE, result.getStatus());
        verify(repository, never()).updatePolicyCountByStatus(any(), anyInt());
    }

    @Test
    void deletePolicy_deletesAndUpdatesCount_whenFound() {
        Policy existing = policy(12L, "A", Status.INACTIVE, null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(existing));
        when(repository.countByStatus(Status.INACTIVE)).thenReturn(2);

        service.deletePolicy(12L);

        verify(repository).deleteById(12L);
        verify(repository).updatePolicyCountByStatus(Status.INACTIVE, 2);
    }

    @Test
    void deletePolicy_throws_whenNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> service.deletePolicy(100L));
    }

    @Test
    void getAllPoliciesByPropertyId_throws_whenBlank() {
        PolicyValidationException ex = assertThrows(PolicyValidationException.class,
                () -> service.getAllPoliciesByPropertyId(" "));
        assertTrue(ex.getMessage().contains("Property ID"));
    }

    @Test
    void getAllPoliciesByPropertyId_returnsData_whenValid() {
        Policy p = policy(13L, "A", Status.ACTIVE, "PR-10", "PC-10");
        when(repository.findAllByPropertyId("PR-10")).thenReturn(List.of(p));

        PolicyListResponse response = service.getAllPoliciesByPropertyId("PR-10");

        assertEquals(1, response.getPolicies().size());
        assertEquals("PR-10", response.getPolicies().get(0).getPropertyId());
    }

    @Test
    void mapPolicyToProperty_throws_whenPropertyIdBlank() {
        assertThrows(PolicyValidationException.class, () -> service.mapPolicyToProperty(1L, ""));
    }

    @Test
    void mapPolicyToProperty_throws_whenPolicyMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> service.mapPolicyToProperty(1L, "P-1"));
    }

    @Test
    void mapPolicyToProperty_mapsAndReturnsUpdatedPolicy() {
        Policy policy = policy(1L, "A", Status.ACTIVE, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(policy));

        PropertyDto property = new PropertyDto();
        property.setId("P-1");
        property.setPropertyCode("PC-1");
        when(propertyClient.getPropertyById("P-1")).thenReturn(property);
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyDto result = service.mapPolicyToProperty(1L, "P-1");

        assertEquals("P-1", result.getPropertyId());
        assertEquals("PC-1", result.getPropertyCode());
    }

    @Test
    void unmapPolicyFromProperty_throws_whenPolicyMissing() {
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> service.unmapPolicyFromProperty(2L));
    }

    @Test
    void unmapPolicyFromProperty_clearsPropertyFields() {
        Policy policy = policy(2L, "B", Status.ACTIVE, "P-2", "PC-2");
        when(repository.findById(2L)).thenReturn(Optional.of(policy));
        when(repository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyDto result = service.unmapPolicyFromProperty(2L);

        assertNull(result.getPropertyId());
        assertNull(result.getPropertyCode());
    }

    @Test
    void getAllPolicies_delegatesOverload() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findAllByStatus(Status.ACTIVE)).thenReturn(List.of());
        when(repository.findAllByStatus(Status.DRAFT)).thenReturn(List.of());
        when(repository.findAllByStatus(Status.INACTIVE)).thenReturn(List.of());

        PolicyListResponse response = service.getAllPolicies();

        assertNotNull(response);
        verify(repository, times(2)).findAll();
    }

    @Test
    void createPolicy_throwsPolicyValidationWhenPropertyLookupFails() {
        baseDto.setPropertyId("P-ERR");
        when(propertyClient.getPropertyById("P-ERR"))
                .thenThrow(new PolicyValidationException(Map.of("propertyId", "error")));

        assertThrows(PolicyValidationException.class, () -> service.createPolicy(baseDto));

        verify(repository, never()).save(any());
    }

    private Policy policy(Long id, String name, Status status, String propertyId, String propertyCode) {
        Policy p = new Policy();
        p.setId(id);
        p.setPolicyName(name);
        p.setPolicyType("Type");
        p.setServiceType("Service");
        p.setUsedBy("Team");
        p.setPolicyCode("CODE-" + id);
        p.setPolicyCategory("Cat");
        p.setStatus(status);
        p.setCreatedBy("creator");
        p.setPropertyId(propertyId);
        p.setPropertyCode(propertyCode);
        return p;
    }
}
