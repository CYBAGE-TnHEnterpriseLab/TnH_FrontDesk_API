package com.pms.inventory.inventory.dto.response;


public record PropertyDeletionCheckResponse(
        String propertyId,
        boolean canDelete,
        boolean hasActiveReservations
) {
}