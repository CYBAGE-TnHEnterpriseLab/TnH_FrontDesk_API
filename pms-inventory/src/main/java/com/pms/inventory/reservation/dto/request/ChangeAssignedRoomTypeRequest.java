package com.pms.inventory.reservation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeAssignedRoomTypeRequest(
        @NotNull(message = "assignedRoomTypeId is required")
        UUID assignedRoomTypeId
) {
}

