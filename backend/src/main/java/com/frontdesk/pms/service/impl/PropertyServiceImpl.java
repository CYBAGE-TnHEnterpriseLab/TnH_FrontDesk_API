package com.frontdesk.pms.service.impl;

import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.mapper.PropertyMapper;
import com.frontdesk.pms.repository.PropertyRepository;
import com.frontdesk.pms.service.PropertyService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyServiceImpl(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public PropertyResponseDTO createProperty(PropertyRequestDTO requestDTO) {

        // 🔥 FRD Rule: Property name must be unique
        propertyRepository.findByPropertyName(requestDTO.getPropertyName())
                .ifPresent(p -> {
                    throw new RuntimeException("Property name already exists");
                });

        // Convert DTO → Entity
        Property property = PropertyMapper.toEntity(requestDTO);

        // Save to DB
        Property savedProperty = propertyRepository.save(property);

        // Convert Entity → Response
        return PropertyMapper.toResponse(savedProperty);
    }

    @Override
    public List<PropertyResponseDTO> getAllProperties() {
    return propertyRepository.findAll()
            .stream()
            .map(PropertyMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public PropertyResponseDTO getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Property not found"));

         return PropertyMapper.toResponse(property);
    }
}