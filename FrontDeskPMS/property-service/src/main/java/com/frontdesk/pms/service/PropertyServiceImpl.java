package com.frontdesk.pms.service;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.exception.PropertyNotFoundException;
import com.frontdesk.pms.mapper.PropertyMapper;
import com.frontdesk.pms.repository.PropertyRepository;
import com.frontdesk.pms.repository.PropertySpecifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository repository;

    @Override
    public PropertyResponseDTO createDraft(PropertyRequestDTO request) {
        log.debug("Creating property: name='{}', email='{}', timeZone='{}'", request.getName(), request.getEmail(), request.getTimeZone());

        Property property = PropertyMapper.toEntity(request);

        Property saved = repository.save(property);
        log.info("Created property: id={}, name='{}', status={}", saved.getId(), saved.getName(), saved.getStatus());

        return PropertyMapper.toDto(saved);
    }

    @Override
    public PropertyResponseDTO updateProperty(UUID propertyId, PropertyRequestDTO request) {
        log.debug("Updating property: id={}", propertyId);
        Property property = repository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        boolean anyChange = PropertyMapper.updateProperty(property, request);

        if (!anyChange) {
            log.warn("Update property called with no changes: id={}", propertyId);
        }

        Property saved = repository.save(property);
        log.info("Updated property: id={}, name='{}', status={}", saved.getId(), saved.getName(), saved.getStatus());

        return PropertyMapper.toDto(saved);
    }

    @Override
    public PropertyResponseDTO findPropertiesByUUID(UUID propertyId) {
        log.debug("Finding property by id: id={}", propertyId);
        Property property = repository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
        return PropertyMapper.toDto(property);
    }

    @Override
    public void deleteProperty(UUID propertyId) {
        log.debug("Deleting property: id={}", propertyId);
        Property property = repository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
        repository.delete(property);
        log.info("Deleted property: id={}", propertyId);
    }

    @Override
    public List<PropertyResponseDTO> getAllProperties() {
        log.debug("Fetching all properties");
        List<Property> results = repository.findAll();
        log.debug("Fetched all properties size={}", results.size());
        return results.stream().map(PropertyMapper::toDto).toList();
    }

    @Override
    public List<PropertyResponseDTO> findPropertiesByName(String name) {
        if (name == null || name.isBlank()) {
            log.warn("Find properties by name called with blank name");
            return List.of();
        }

        String trimmed = name.trim();
        log.debug("Finding properties by name (ignore case): name='{}'", trimmed);
        List<Property> results = repository.findByNameIgnoreCase(trimmed);
        log.debug("Find by name result size={}", results.size());
        return results.stream().map(PropertyMapper::toDto).toList();
    }

    @Override
    public List<PropertyResponseDTO> searchProperties(
            String name,
            String timeZone,
            LocalTime checkInFrom,
            LocalTime checkInTo,
            PropertyStatus status
    ) {
        log.debug("Searching properties: name='{}', timeZone='{}', checkInFrom={}, checkInTo={}, status={}",
                name, timeZone, checkInFrom, checkInTo, status);
        Specification<Property> spec = (root, query, cb) -> cb.conjunction();

        if (name != null && !name.isBlank()) {
            spec = spec.and(PropertySpecifications.nameContainsIgnoreCase(name.trim()));
        }
        if (timeZone != null && !timeZone.isBlank()) {
            spec = spec.and(PropertySpecifications.timeZoneEquals(timeZone.trim()));
        }
        if (status != null) {
            spec = spec.and(PropertySpecifications.statusEquals(status));
        }
        if (checkInFrom != null) {
            spec = spec.and(PropertySpecifications.checkInTimeGte(checkInFrom));
        }
        if (checkInTo != null) {
            spec = spec.and(PropertySpecifications.checkInTimeLte(checkInTo));
        }

        List<Property> results = repository.findAll(spec);
        log.debug("Search properties result size={}", results.size());
        return results.stream().map(PropertyMapper::toDto).toList();
    }

    @Override
    public PropertyResponseDTO publish(UUID propertyId) {

        Property property = repository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (property.getStatus() != PropertyStatus.DRAFT) {
            throw new RuntimeException("Only draft can be published");
        }

        property.setStatus(PropertyStatus.PUBLISHED);

        return PropertyMapper.toDto(repository.save(property));
    }
}
