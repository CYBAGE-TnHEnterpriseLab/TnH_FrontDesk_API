package com.frontdesk.pms.mapper;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.entity.Property;
import com.frontdesk.pms.exception.BadRequestException;

/**
 * Simple mapping helpers between Property DTOs and entity.
 */
public final class PropertyMapper {

    private PropertyMapper() {
    }

    public static Property toEntity(PropertyRequestDTO request) {
        if (request == null) {
            return null;
        }

        return Property.builder()
                .name(trim(request.getName()))
                .email(trim(request.getEmail()))
                .address(trim(request.getAddress()))
                .contactName(trim(request.getContactName()))
                .contactNumber(trim(request.getContactNumber()))
                .timeZone(trim(request.getTimeZone()))
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .nightAuditTime(request.getNightAuditTime())
                .status(PropertyStatus.DRAFT)
                .build();
    }

    public static PropertyResponseDTO toDto(Property entity) {
        if (entity == null) {
            return null;
        }

        return PropertyResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .contactName(entity.getContactName())
                .contactNumber(entity.getContactNumber())
                .timeZone(entity.getTimeZone())
                .checkInTime(entity.getCheckInTime())
                .checkOutTime(entity.getCheckOutTime())
                .nightAuditTime(entity.getNightAuditTime())
                .status(entity.getStatus())
                .build();
    }

    /**
     * Applies a PATCH-like update from the request onto an existing entity.
     *
     * @return true if any field was updated, false otherwise.
     */
    public static boolean updateProperty(Property property, PropertyRequestDTO request) {
        if (property == null) {
            throw new IllegalArgumentException("property must not be null");
        }
        if (request == null) {
            return false;
        }

        boolean anyChange = false;

        if (request.getName() != null) {
            property.setName(requireNonBlankTrimmed("name", request.getName()));
            anyChange = true;
        }
        if (request.getEmail() != null) {
            property.setEmail(requireNonBlankTrimmed("email", request.getEmail()));
            anyChange = true;
        }
        if (request.getAddress() != null) {
            property.setAddress(requireNonBlankTrimmed("address", request.getAddress()));
            anyChange = true;
        }
        if (request.getContactName() != null) {
            property.setContactName(requireNonBlankTrimmed("contactName", request.getContactName()));
            anyChange = true;
        }
        if (request.getContactNumber() != null) {
            property.setContactNumber(requireNonBlankTrimmed("contactNumber", request.getContactNumber()));
            anyChange = true;
        }
        if (request.getTimeZone() != null) {
            property.setTimeZone(requireNonBlankTrimmed("timeZone", request.getTimeZone()));
            anyChange = true;
        }
        if (request.getCheckInTime() != null) {
            property.setCheckInTime(request.getCheckInTime());
            anyChange = true;
        }
        if (request.getCheckOutTime() != null) {
            property.setCheckOutTime(request.getCheckOutTime());
            anyChange = true;
        }
        if (request.getNightAuditTime() != null) {
            property.setNightAuditTime(request.getNightAuditTime());
            anyChange = true;
        }
        return anyChange;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String requireNonBlankTrimmed(String field, String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw new BadRequestException(field + " must not be blank");
        }
        return trimmed;
    }
}
