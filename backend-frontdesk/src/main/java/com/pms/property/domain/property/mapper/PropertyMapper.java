package com.pms.property.domain.property.mapper;

import com.pms.property.domain.property.dto.PropertyResponse;
import com.pms.property.domain.property.entity.PropertyEntity;

public final class PropertyMapper {

    private PropertyMapper() {
    }

    public static PropertyResponse toResponse(PropertyEntity entity) {
        return new PropertyResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getPropertyCode(),
            entity.getPropertyType(),
            entity.getTotalNoOfRooms(),
            entity.getTotalNoOfFloors(),
            entity.getAddress(),
            entity.getCity(),
            entity.getState(),
            entity.getCountry(),
            entity.getZipCode(),
            entity.getWebsite(),
            entity.getContactName(),
            entity.getContactNumber(),
            entity.getTimeZone(),
            entity.getNightAuditTime(),
            entity.getCheckInTime(),
            entity.getCheckOutTime(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getCreatedBy()
        );
    }
}

