package Policy_Management.Policy.dto;

import Policy_Management.Policy.entity.Policy;

public class PolicyMapper {

    public static PolicyDto toDto(Policy p) {
        if (p == null) {
            return null;
        }
        PolicyDto dto = new PolicyDto();
        dto.setId(p.getId());
        dto.setPolicyName(p.getPolicyName());
        dto.setPolicyType(p.getPolicyType());
        dto.setServiceType(p.getServiceType());
        dto.setUsedBy(p.getUsedBy());
        dto.setPolicyCode(p.getPolicyCode());
        dto.setPolicyCategory(p.getPolicyCategory());
        dto.setPriority(p.getPriority());
        dto.setDescription(p.getDescription());
        dto.setEffectiveDate(p.getEffectiveDate());
        dto.setEffectiveTo(p.getEffectiveTo());
        dto.setStatus(p.getStatus());
        dto.setCreatedBy(p.getCreatedBy());
        dto.setAction(p.getAction());
        dto.setPolicyCount(p.getPolicyCount());
        dto.setPropertyId(p.getPropertyId());
        dto.setPropertyCode(p.getPropertyCode());
        return dto;
    }

    public static Policy toEntity(PolicyDto dto) {
        if (dto == null) {
            return null;
        }
        Policy p = new Policy();
        p.setPolicyName(dto.getPolicyName());
        p.setPolicyType(dto.getPolicyType());
        p.setServiceType(dto.getServiceType());
        p.setUsedBy(dto.getUsedBy());
        p.setPolicyCode(dto.getPolicyCode());
        p.setPolicyCategory(dto.getPolicyCategory());
        p.setPriority(dto.getPriority());
        p.setDescription(dto.getDescription());
        p.setEffectiveDate(dto.getEffectiveDate());
        p.setEffectiveTo(dto.getEffectiveTo());
        p.setStatus(dto.getStatus());
        p.setCreatedBy(dto.getCreatedBy());
        p.setAction(dto.getAction());
        p.setPolicyCount(dto.getPolicyCount());
        p.setPropertyId(dto.getPropertyId());
        p.setPropertyCode(dto.getPropertyCode());
        return p;
    }

    public static void copyToEntity(PolicyDto dto, Policy p) {
        if (dto == null || p == null) {
            return;
        }
        if (dto.getPolicyName() != null) p.setPolicyName(dto.getPolicyName());
        if (dto.getPolicyType() != null) p.setPolicyType(dto.getPolicyType());
        if (dto.getServiceType() != null) p.setServiceType(dto.getServiceType());
        if (dto.getUsedBy() != null) p.setUsedBy(dto.getUsedBy());
        if (dto.getPolicyCode() != null) p.setPolicyCode(dto.getPolicyCode());
        if (dto.getPolicyCategory() != null) p.setPolicyCategory(dto.getPolicyCategory());
        if (dto.getPriority() != null) p.setPriority(dto.getPriority());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getEffectiveDate() != null) p.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getEffectiveTo() != null) p.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getStatus() != null) p.setStatus(dto.getStatus());
        if (dto.getCreatedBy() != null) p.setCreatedBy(dto.getCreatedBy());
        if (dto.getAction() != null) p.setAction(dto.getAction());
        if (dto.getPolicyCount() != 0) p.setPolicyCount(dto.getPolicyCount());
        if (dto.getPropertyId() != null) p.setPropertyId(dto.getPropertyId());
        if (dto.getPropertyCode() != null) p.setPropertyCode(dto.getPropertyCode());
    }
}
