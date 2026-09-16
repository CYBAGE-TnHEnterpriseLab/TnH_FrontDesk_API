package com.pms.inventory.reservation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeAssignedRoomTypeRequest(
        @NotNull(message = "assignedRoomTypeId is required")
        String assignedRoomTypeId
) {
}

