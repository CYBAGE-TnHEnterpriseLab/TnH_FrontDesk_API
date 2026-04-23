package com.frontdesk.pms.mapper;

import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.enums.PropertyStatus;

import java.time.LocalDateTime;

public class PropertyMapper {

    // Convert Request DTO → Entity
    public static Property toEntity(PropertyRequestDTO dto) {
        return Property.builder()
                .propertyName(dto.getPropertyName())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .contact(dto.getContact())
                .timezone(dto.getTimezone())
                .nightAuditTime(dto.getNightAuditTime())
                .checkinTime(dto.getCheckinTime())
                .checkoutTime(dto.getCheckoutTime())
                .status(PropertyStatus.DRAFT) // Always default
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Convert Entity → Response DTO
    public static PropertyResponseDTO toResponse(Property property) {
        return PropertyResponseDTO.builder()
                .id(property.getId())
                .propertyName(property.getPropertyName())
                .email(property.getEmail())
                .address(property.getAddress())
                .contact(property.getContact())
                .timezone(property.getTimezone())
                .nightAuditTime(property.getNightAuditTime())
                .checkinTime(property.getCheckinTime())
                .checkoutTime(property.getCheckoutTime())
                .status(property.getStatus())
                .createdAt(property.getCreatedAt())
                .build();
    }
}