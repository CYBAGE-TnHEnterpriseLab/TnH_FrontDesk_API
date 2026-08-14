package com.pms.inventory.inventory.dto.response;

import java.util.UUID;

public record PropertyDeletionCheckResponse(
        UUID propertyId,
        boolean canDelete,
        boolean hasActiveReservations
) {
}