package com.pms.property.domain.property.dto;

import java.util.UUID;

public record PropertyDeletionCheckResponse(
        UUID propertyId,
        boolean canDelete,
        boolean hasActiveReservations
) {
}