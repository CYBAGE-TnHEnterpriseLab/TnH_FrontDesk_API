package com.pms.property.domain.property.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyResponse(
    String id,
    String title,
    String propertyCode,
    String propertyType,
    Integer totalNoOfRooms,
    Integer totalNoOfFloors,
    String address,
    String city,
    String state,
    String country,
    String zipCode,
    String website,
    String contactName,
    String contactNumber,
    String timeZone,
    String nightAuditTime,
    String checkInTime,
    String checkOutTime,
    String status,
    LocalDateTime createdAt,
    UUID createdBy
) {
}

