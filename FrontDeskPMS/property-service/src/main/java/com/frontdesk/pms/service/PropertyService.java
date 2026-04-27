package com.frontdesk.pms.service;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface PropertyService {

    PropertyResponseDTO createDraft(PropertyRequestDTO request);

    PropertyResponseDTO updateProperty(UUID propertyId, PropertyRequestDTO request);

    PropertyResponseDTO findPropertiesByUUID(UUID propertyId);

    void deleteProperty(UUID propertyId);

    List<PropertyResponseDTO> getAllProperties();

    List<PropertyResponseDTO> findPropertiesByName(String name);

    List<PropertyResponseDTO> searchProperties(
            String name,
            String timeZone,
            LocalTime checkInFrom,
            LocalTime checkInTo,
            PropertyStatus status
    );

    PropertyResponseDTO  publish(UUID id);

}
