package com.pms.property.domain.property.service;

import com.pms.property.domain.property.dto.PropertyResponse;
import java.util.List;
import java.util.UUID;

public interface PropertyService {

    PropertyResponse getById(String propertyId);

    List<PropertyResponse> listByCreator(UUID creator);

    void deleteOwnedProperty(String propertyId, UUID actor);
}


