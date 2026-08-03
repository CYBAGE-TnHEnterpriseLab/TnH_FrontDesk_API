package Policy_Management.Policy.service.impl;

import Policy_Management.Policy.dto.PolicyDto;
import Policy_Management.Policy.dto.PolicyListResponse;
import Policy_Management.Policy.dto.PolicyMapper;
import Policy_Management.Policy.dto.PropertyDto;
import Policy_Management.Policy.dto.Status;
import Policy_Management.Policy.entity.Policy;
import Policy_Management.Policy.exception.DuplicatePolicyException;
import Policy_Management.Policy.exception.PolicyNotFoundException;
import Policy_Management.Policy.exception.PolicyValidationException;
import Policy_Management.Policy.repository.PolicyRepository;
import Policy_Management.Policy.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);
    private final PolicyRepository repository;

    @Autowired
    public PolicyServiceImpl(PolicyRepository repository) {
        this.repository = repository;
    }

    @Autowired
    PropertyClient propertyClient;

    
    @Override
    @Transactional
    public PolicyDto createPolicy(PolicyDto dto) {
        LOGGER.info("Service createPolicy called with dto={}", dto);
        if (dto.getStatus() == null) {
            dto.setStatus(Status.DRAFT);
        }
        enrichPropertyDetailsWithDraftFallback(dto);
        validateForStatus(dto);
        if (repository.existsByPolicyCode(dto.getPolicyCode())) {
            LOGGER.warn("Duplicate policy creation attempt for policyCode={}", dto.getPolicyCode());
            throw new DuplicatePolicyException(dto);
        }
        Policy p = PolicyMapper.toEntity(dto);
        p.setPolicyCount(calculatePolicyCountForStatus(p.getStatus()) + 1);
        Policy saved = repository.save(p);
        repository.updatePolicyCountByStatus(p.getStatus(), (int) calculatePolicyCountForStatus(p.getStatus()));
        PolicyDto result = PolicyMapper.toDto(saved);
        LOGGER.info("Service createPolicy returning {}", result);
        return result;
    }

    @Override
    public PolicyListResponse getAllPolicies() {
        return getAllPolicies(null);
    }

    @Override
    public PolicyListResponse getAllPolicies(Status status) {
        LOGGER.info("Service getAllPolicies called with status={}", status);
        List<Policy> policies;
        if (status == null) {
            policies = repository.findAll();
        } else {
            policies = repository.findAllByStatus(status);
        }

        List<PolicyDto> policyDtos = policies.stream().map(this::toDto).
        collect(Collectors.toList());
        PolicyListResponse response = new PolicyListResponse();
        response.setPolicies(policyDtos);
        response.setTotalPolicies(repository.findAll().size());
        response.setActivePolicies((int) repository.findAllByStatus(Status.ACTIVE).size());
        response.setDraftPolicies((int) repository.findAllByStatus(Status.DRAFT).size());
        response.setInactivePolicies((int) repository.findAllByStatus(Status.INACTIVE).size());
        LOGGER.info("Service getAllPolicies returning response={}", response);
        return response;
    }

    @Override
    public PolicyDto getPolicyById(Long id) {
        LOGGER.info("Service getPolicyById called with id={}", id);
        Policy p = repository.findById(id).orElseThrow(() -> {
            LOGGER.warn("Policy not found for id={}", id);
            return new PolicyNotFoundException(id);
        });
        PolicyDto result = toDto(p);
        LOGGER.info("Service getPolicyById returning {}", result);
        return result;
    }

    @Override
    @Transactional
    public PolicyDto updatePolicy(Long id, PolicyDto dto) {
        LOGGER.info("Service updatePolicy called with id={} dto={}", id, dto);
        Optional<Policy> opt = repository.findById(id);
        if (!opt.isPresent()) {
            LOGGER.warn("Policy not found for update id={}", id);
            throw new PolicyNotFoundException(id);
        }
        Policy existing = opt.get();
        Status oldStatus = existing.getStatus();

        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getPropertyId() == null) {
            dto.setPropertyId(existing.getPropertyId());
        }
        enrichPropertyDetailsWithDraftFallback(dto);
        
        PolicyMapper.copyToEntity(dto, existing);
        validateForStatus(PolicyMapper.toDto(existing));
        Policy saved = repository.save(existing);

        if (!saved.getStatus().equals(oldStatus)) {
            repository.updatePolicyCountByStatus(oldStatus, (int) calculatePolicyCountForStatus(oldStatus));
            repository.updatePolicyCountByStatus(saved.getStatus(), (int) calculatePolicyCountForStatus(saved.getStatus()));
        }

        

        PolicyDto result = PolicyMapper.toDto(saved);
        LOGGER.info("Service updatePolicy returning {}", result);
        return result;
    }

    @Override
    @Transactional
    public void deletePolicy(Long id) {
        LOGGER.info("Service deletePolicy called with id={}", id);
        Policy existing = repository.findById(id).orElseThrow(() -> {
            LOGGER.warn("Policy not found for delete id={}", id);
            return new PolicyNotFoundException(id);
        });
        Status status = existing.getStatus();
        repository.deleteById(id);
        repository.updatePolicyCountByStatus(status, (int) calculatePolicyCountForStatus(status));
        LOGGER.info("Service deletePolicy completed for id={} status={}", id, status);
    }

    @SuppressWarnings("unused")
    private Policy toEntity(PolicyDto dto) {
        return PolicyMapper.toEntity(dto);
    }

    @SuppressWarnings("unused")
    private void copyToEntity(PolicyDto dto, Policy p) {
        PolicyMapper.copyToEntity(dto, p);
    }

    private PolicyDto toDto(Policy p) {
        return PolicyMapper.toDto(p);
    }

    private int calculatePolicyCountForStatus(Status status) {
        return repository.countByStatus(status);
    }

    private void validateForStatus(PolicyDto dto) {
        Status status = dto.getStatus();
        if (status == null) {
            status = Status.DRAFT;
            dto.setStatus(status);
        }
        if (Status.PUBLISHED.equals(status)) {
            Map<String, String> errors = new HashMap<>();
            if (dto.getPolicyName() == null || dto.getPolicyName().isBlank()) {
                errors.put("policyName", "Policy name is required for published policies");
            }
            if (dto.getPolicyType() == null || dto.getPolicyType().isBlank()) {
                errors.put("policyType", "Policy type is required for published policies");
            }
            if (dto.getServiceType() == null || dto.getServiceType().isBlank()) {
                errors.put("serviceType", "Service type is required for published policies");
            }
            if (dto.getUsedBy() == null || dto.getUsedBy().isBlank()) {
                errors.put("usedBy", "Used by is required for published policies");
            }
            if (dto.getPolicyCode() == null || dto.getPolicyCode().isBlank()) {
                errors.put("policyCode", "Policy code is required for published policies");
            }
            if (dto.getPolicyCategory() == null || dto.getPolicyCategory().isBlank()) {
                errors.put("policyCategory", "Policy category is required for published policies");
            }
            if (dto.getCreatedBy() == null || dto.getCreatedBy().isBlank()) {
                errors.put("createdBy", "Created by is required for published policies");
            }
            if (dto.getPropertyId() == null) {
                errors.put("propertyId", "Property id is required for published policies");
            }
            if (!errors.isEmpty()) {
                throw new PolicyValidationException(errors);
            }
        }
    }

    private void enrichPropertyDetails(PolicyDto dto) {
        if (dto.getPropertyId() == null) {
            return;
        }
        PropertyDto property = propertyClient.getPropertyById(dto.getPropertyId());
        dto.setPropertyCode(property.getPropertyCode());
    }

    private void enrichPropertyDetailsWithDraftFallback(PolicyDto dto) {
        try {
            enrichPropertyDetails(dto);
        } catch (PolicyValidationException ex) {
            Status status = dto.getStatus() == null ? Status.DRAFT : dto.getStatus();
            if (Status.PUBLISHED.equals(status)) {
                throw ex;
            }
            LOGGER.warn("Property API lookup failed for DRAFT policy. Continuing without propertyCode. errors={}", ex.getErrors());
        }
    }

   
}
