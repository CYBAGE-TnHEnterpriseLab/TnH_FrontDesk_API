package com.pms.property.integration.inventory.service;

import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.integration.inventory.dto.InventoryReconciliationRequest;
import com.pms.property.integration.inventory.dto.RoomMasterSyncRequest;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventorySyncPayloadMapper {

    private final int horizonDays;

    public InventorySyncPayloadMapper(@Value("${inventory.sync.horizon-days:365}") int horizonDays) {
        this.horizonDays = horizonDays;
    }

    public InventoryReconciliationRequest toRequest(String propertyId, List<RoomOutletTypeEntity> roomTypes) {
        UUID propertyUuid = parsePropertyUuid(propertyId);
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(Math.max(horizonDays, 1));

        List<InventoryReconciliationRequest.RoomTypeInventoryInput> normalizedRoomTypes = roomTypes.stream()
            .sorted(Comparator.comparing(entity -> safe(entity.getRoomName()), String.CASE_INSENSITIVE_ORDER))
            .map(roomType -> new InventoryReconciliationRequest.RoomTypeInventoryInput(
                roomTypeUuid(propertyId, roomType),
                Boolean.TRUE.equals(roomType.getAvailableForSell()) ? safeQuantity(roomType.getQuantity()) : 0
            ))
            .toList();

        return new InventoryReconciliationRequest(propertyUuid, fromDate, toDate, normalizedRoomTypes);
    }

    public RoomMasterSyncRequest toRoomMasterSyncRequest(
        String propertyId,
        List<InventoryRoomEntity> rooms,
        List<RoomOutletTypeEntity> roomTypes
    ) {
        UUID propertyUuid = parsePropertyUuid(propertyId);
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(Math.max(horizonDays, 1));
        Map<String, RoomOutletTypeEntity> outletTypeByName = buildRoomTypeByName(roomTypes);

        List<RoomMasterSyncRequest.RoomMasterUnit> normalizedRooms = rooms.stream()
            .sorted(Comparator.comparing(entity -> safe(entity.getRoomNumber()), String.CASE_INSENSITIVE_ORDER))
            .map(room -> new RoomMasterSyncRequest.RoomMasterUnit(
                resolveRoomTypeUuid(propertyId, room.getRoomTypeName(), outletTypeByName),
                safe(room.getRoomTypeName()),
                safe(room.getRoomNumber()),
                safe(room.getFloorName()),
                null,
                null,
                null,
                false,
                true
            ))
            .toList();

        return new RoomMasterSyncRequest(propertyUuid, fromDate, toDate, normalizedRooms);
    }

    private Map<String, RoomOutletTypeEntity> buildRoomTypeByName(List<RoomOutletTypeEntity> roomTypes) {
        Map<String, RoomOutletTypeEntity> outletTypeByName = new HashMap<>();
        for (RoomOutletTypeEntity roomType : roomTypes) {
            String key = safe(roomType.getRoomName()).toLowerCase(Locale.ROOT);
            if (!key.isBlank()) {
                outletTypeByName.putIfAbsent(key, roomType);
            }
        }
        return outletTypeByName;
    }

    private UUID resolveRoomTypeUuid(String propertyId, String roomTypeName, Map<String, RoomOutletTypeEntity> outletTypeByName) {
        RoomOutletTypeEntity roomType = outletTypeByName.get(safe(roomTypeName).toLowerCase(Locale.ROOT));
        return roomType == null
            ? roomTypeUuidFromName(propertyId, roomTypeName)
            : roomTypeUuid(propertyId, roomType);
    }

    private UUID parsePropertyUuid(String propertyId) {
        try {
            return UUID.fromString(propertyId);
        } catch (IllegalArgumentException ex) {
            return deterministicUuid("property", propertyId);
        }
    }

    private UUID roomTypeUuid(String propertyId, RoomOutletTypeEntity roomType) {
        String roomKey = roomType.getRoomCode();
        if (roomKey == null || roomKey.isBlank()) {
            roomKey = safe(roomType.getRoomName());
        }
        return deterministicUuid(propertyId, roomKey.isBlank() ? "unknown" : roomKey);
    }

    public UUID roomTypeUuidFromName(String propertyId, String roomTypeName) {
        String roomKey = safe(roomTypeName);
        return deterministicUuid(propertyId, roomKey.isBlank() ? "unknown" : roomKey);
    }

    private UUID deterministicUuid(String namespace, String value) {
        String payload = (namespace + ":" + value).toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null || quantity < 0 ? 0 : quantity;
    }
}


