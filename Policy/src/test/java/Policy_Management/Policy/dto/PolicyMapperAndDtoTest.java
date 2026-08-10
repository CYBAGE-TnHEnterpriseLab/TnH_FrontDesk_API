package Policy_Management.Policy.dto;

import Policy_Management.Policy.entity.Policy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class PolicyMapperAndDtoTest {

    @Test
    void toDto_andToEntity_mapsAllFields() {
        Policy entity = new Policy();
        entity.setId(10L);
        entity.setPolicyName("Policy A");
        entity.setPolicyType("Type A");
        entity.setServiceType("Service A");
        entity.setUsedBy("Users");
        entity.setPolicyCode("C-10");
        entity.setPolicyCategory("Cat");
        entity.setPriority(1);
        entity.setDescription("desc");
        entity.setEffectiveDate(LocalDate.of(2026, 1, 1));
        entity.setEffectiveTo(LocalDate.of(2026, 12, 31));
        entity.setStatus(Status.ACTIVE);
        entity.setCreatedBy("creator");
        entity.setAction("CREATE");
        entity.setPolicyCount(8);
        entity.setPropertyId("P-1");
        entity.setPropertyCode("PC-1");

        PolicyDto dto = PolicyMapper.toDto(entity);

        assertEquals(10L, dto.getId());
        assertEquals("Policy A", dto.getPolicyName());
        assertEquals("C-10", dto.getPolicyCode());
        assertEquals(Status.ACTIVE, dto.getStatus());
        assertEquals("P-1", dto.getPropertyId());
        assertEquals("PC-1", dto.getPropertyCode());

        Policy remappedEntity = PolicyMapper.toEntity(dto);
        assertEquals("Policy A", remappedEntity.getPolicyName());
        assertEquals("Type A", remappedEntity.getPolicyType());
        assertEquals("PC-1", remappedEntity.getPropertyCode());
    }

    @Test
    void toDto_andToEntity_returnsNull_whenInputNull() {
        assertNull(PolicyMapper.toDto(null));
        assertNull(PolicyMapper.toEntity(null));
    }

    @Test
    void toPropertyDto_mapsPropertyFields() {
        PropertyDto propertyDto = new PropertyDto();
        propertyDto.setId("P-2");
        propertyDto.setPropertyCode("PC-2");

        Policy mapped = PolicyMapper.toPropertyDto(propertyDto);

        assertEquals("P-2", mapped.getPropertyId());
        assertEquals("PC-2", mapped.getPropertyCode());
    }

    @Test
    void toPropertyDto_returnsNull_whenInputNull() {
        assertNull(PolicyMapper.toPropertyDto(null));
    }

    @Test
    void copyToEntity_updatesNonNullFields_only() {
        PolicyDto dto = new PolicyDto();
        dto.setPolicyName("Updated");
        dto.setPriority(9);
        dto.setPolicyCount(5);
        dto.setPropertyId("PR-9");
        dto.setPropertyCode("PC-9");

        Policy entity = new Policy();
        entity.setPolicyType("Type");
        entity.setPolicyCount(1);

        PolicyMapper.copyToEntity(dto, entity);

        assertEquals("Updated", entity.getPolicyName());
        assertEquals("Type", entity.getPolicyType());
        assertEquals(9, entity.getPriority());
        assertEquals(5, entity.getPolicyCount());
        assertEquals("PR-9", entity.getPropertyId());
        assertEquals("PC-9", entity.getPropertyCode());
    }

    @Test
    void copyToEntity_noOp_whenArgumentsNull() {
        PolicyMapper.copyToEntity(null, new Policy());
        PolicyMapper.copyToEntity(new PolicyDto(), null);
    }

    @Test
    void copyToEntity_updatesAllConditionalFields_whenPresent() {
        PolicyDto dto = new PolicyDto();
        dto.setPolicyName("N");
        dto.setPolicyType("T");
        dto.setServiceType("S");
        dto.setUsedBy("U");
        dto.setPolicyCode("C");
        dto.setPolicyCategory("CAT");
        dto.setPriority(2);
        dto.setDescription("D");
        dto.setEffectiveDate(LocalDate.of(2026, Month.JANUARY, 1));
        dto.setEffectiveTo(LocalDate.of(2026, Month.DECEMBER, 31));
        dto.setStatus(Status.PUBLISHED);
        dto.setCreatedBy("CB");
        dto.setAction("ACT");
        dto.setPolicyCount(7);
        dto.setPropertyId("PID");
        dto.setPropertyCode("PCODE");

        Policy entity = new Policy();
        PolicyMapper.copyToEntity(dto, entity);

        assertEquals("N", entity.getPolicyName());
        assertEquals("T", entity.getPolicyType());
        assertEquals("S", entity.getServiceType());
        assertEquals("U", entity.getUsedBy());
        assertEquals("C", entity.getPolicyCode());
        assertEquals("CAT", entity.getPolicyCategory());
        assertEquals(2, entity.getPriority());
        assertEquals("D", entity.getDescription());
        assertEquals(LocalDate.of(2026, Month.JANUARY, 1), entity.getEffectiveDate());
        assertEquals(LocalDate.of(2026, Month.DECEMBER, 31), entity.getEffectiveTo());
        assertEquals(Status.PUBLISHED, entity.getStatus());
        assertEquals("CB", entity.getCreatedBy());
        assertEquals("ACT", entity.getAction());
        assertEquals(7, entity.getPolicyCount());
        assertEquals("PID", entity.getPropertyId());
        assertEquals("PCODE", entity.getPropertyCode());
    }

    @Test
    void copyToEntity_doesNotOverwritePolicyCount_whenZero() {
        PolicyDto dto = new PolicyDto();
        dto.setPolicyCount(0);
        Policy entity = new Policy();
        entity.setPolicyCount(3);

        PolicyMapper.copyToEntity(dto, entity);

        assertEquals(3, entity.getPolicyCount());
    }

    @Test
    void policyListResponse_calculatesCounts() {
        PolicyDto p1 = new PolicyDto();
        p1.setStatus(Status.ACTIVE);
        PolicyDto p2 = new PolicyDto();
        p2.setStatus(Status.DRAFT);
        PolicyDto p3 = new PolicyDto();
        p3.setStatus(Status.INACTIVE);

        PolicyListResponse response = new PolicyListResponse(java.util.List.of(p1, p2, p3));

        assertEquals(3, response.getTotalPolicies());
        assertEquals(1, response.getActivePolicies());
        assertEquals(1, response.getDraftPolicies());
        assertEquals(1, response.getInactivePolicies());
    }

    @Test
    void apiResponse_gettersSettersAndToString() {
        APIResponse<String> response = new APIResponse<>();
        response.setStatus("success");
        response.setMessage("ok");
        response.setData("payload");

        assertEquals("success", response.getStatus());
        assertEquals("ok", response.getMessage());
        assertEquals("payload", response.getData());
        assertTrue(response.toString().contains("success"));
    }

    @Test
    void status_fromValue_andGetValue_behaviors() {
        assertEquals(Status.ACTIVE, Status.fromValue("active"));
        assertEquals(Status.DRAFT, Status.fromValue(" DRAFT "));
        assertNull(Status.fromValue(""));
        assertNull(Status.fromValue("unknown"));
        assertEquals("ACTIVE", Status.ACTIVE.getValue());
    }
}
