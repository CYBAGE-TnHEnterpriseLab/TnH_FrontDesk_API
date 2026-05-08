package com.frontdesk.pms.account.service;

import com.frontdesk.pms.account.dto.RevenueMappingValidationResponseDTO;
import com.frontdesk.pms.account.dto.RevenueMappingRequestDTO;
import com.frontdesk.pms.account.dto.RevenueMappingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RevenueMappingService {
    RevenueMappingResponseDTO create(UUID propertyId, RevenueMappingRequestDTO request);
    List<RevenueMappingResponseDTO> listByProperty(UUID propertyId);
    RevenueMappingResponseDTO get(UUID propertyId, UUID mappingId);
    RevenueMappingResponseDTO update(UUID propertyId, UUID mappingId, RevenueMappingRequestDTO request);
    void delete(UUID propertyId, UUID mappingId);
    RevenueMappingValidationResponseDTO validatePostingReadiness(UUID propertyId);
}
