package com.pms.inventory.block.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReleaseInventoryBlockRequest(
        @NotNull(message = "blockId is required")
        Long blockId
) {
}

