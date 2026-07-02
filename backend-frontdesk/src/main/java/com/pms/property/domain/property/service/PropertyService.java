package com.pms.property.domain.property.service;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.property.dto.PropertyResponse;
import com.pms.property.domain.property.mapper.PropertyMapper;
import com.pms.property.domain.property.repository.PropertyRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(String propertyId) {
        return propertyRepository.findById(propertyId)
            .map(PropertyMapper::toResponse)
            .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> listByCreator(String creator) {
        return propertyRepository.findByCreatedBy(creator)
            .stream()
            .map(PropertyMapper::toResponse)
            .collect(Collectors.toList());
    }
}

