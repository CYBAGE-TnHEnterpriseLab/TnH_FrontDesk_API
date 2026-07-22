package com.pms.inventory.reservation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReleaseInventoryRequest(
        @NotNull(message = "reservationId is required")
        UUID reservationId
) {
}

