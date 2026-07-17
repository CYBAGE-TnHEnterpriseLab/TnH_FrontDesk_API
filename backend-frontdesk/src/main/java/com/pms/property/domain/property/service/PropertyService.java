package com.pms.property.domain.property.service;

import com.pms.property.domain.property.dto.PropertyResponse;
import java.util.List;

public interface PropertyService {

    PropertyResponse getById(String propertyId);

    List<PropertyResponse> listByCreator(String creator);

    void deleteOwnedProperty(String propertyId, String actor);
}


