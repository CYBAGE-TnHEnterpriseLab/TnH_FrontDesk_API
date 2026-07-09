package com.pms.property.domain.property.dto;

import java.time.Instant;

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
    Instant createdAt,
    String createdBy
) {
}

