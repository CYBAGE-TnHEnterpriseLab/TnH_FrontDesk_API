package com.pms.reservation.integration;

import com.pms.reservation.integration.dto.InventoryDeductionRequest;
import com.pms.reservation.integration.dto.InventorySyncRequest;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import java.time.LocalDate;
import java.util.List;

public interface PropertyInventoryPort {

    PropertyInventoryValidationResponse validateInventory(String propertyId, String roomType, Integer requestedRooms);

    void deductInventory(InventoryDeductionRequest request);

    void syncInventory(InventorySyncRequest request);

    List<PropertyRoomInventoryDto> fetchLiveInventory(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType
    );
}
