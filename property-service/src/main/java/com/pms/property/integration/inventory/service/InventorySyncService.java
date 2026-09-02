package com.pms.property.integration.inventory.service;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import com.pms.property.integration.inventory.dto.InventoryReconciliationRequest;
import com.pms.property.integration.inventory.dto.RoomMasterSyncRequest;
import com.pms.property.integration.inventory.dto.InventorySyncStatusResponse;
import com.pms.property.integration.inventory.entity.InventorySyncStateEntity;
import com.pms.property.integration.inventory.entity.InventorySyncStatus;
import com.pms.property.integration.inventory.exception.InventorySyncException;
import com.pms.property.integration.inventory.repository.InventorySyncStateRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class InventorySyncService {

    private final RoomOutletTypeRepository roomOutletTypeRepository;
    private final InventoryRoomRepository inventoryRoomRepository;
    private final InventorySyncPayloadMapper payloadMapper;
    private final InventorySyncClient inventorySyncClient;
    private final InventorySyncStateRepository syncStateRepository;
    private final Executor inventorySyncExecutor;

    public InventorySyncService(
        RoomOutletTypeRepository roomOutletTypeRepository,
        InventoryRoomRepository inventoryRoomRepository,
        InventorySyncPayloadMapper payloadMapper,
        InventorySyncClient inventorySyncClient,
        InventorySyncStateRepository syncStateRepository,
        @Qualifier("inventorySyncExecutor") Executor inventorySyncExecutor
    ) {
        this.roomOutletTypeRepository = roomOutletTypeRepository;
        this.inventoryRoomRepository = inventoryRoomRepository;
        this.payloadMapper = payloadMapper;
        this.inventorySyncClient = inventorySyncClient;
        this.syncStateRepository = syncStateRepository;
        this.inventorySyncExecutor = inventorySyncExecutor;
    }

    public void requestSyncAfterCommit(String propertyId, String authHeader) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            registerAfterCommitSync(propertyId, authHeader);
            markPending(propertyId);
            return;
        }

        syncSilently(propertyId, authHeader);
    }

    public InventorySyncStatusResponse syncNow(String propertyId, String authHeader) {
        return toStatusResponse(synchronizeProperty(propertyId, authHeader));
    }

    public InventorySyncStatusResponse getStatus(String propertyId) {
        return toStatusResponse(loadStateOrThrow(propertyId));
    }

    private void syncSilently(String propertyId, String authHeader) {
        try {
            synchronizeProperty(propertyId, authHeader);
        } catch (InventorySyncException ex) {
            // Sync state is already persisted with FAILED status.
            log.warn("Inventory sync failed for property {}: {}", propertyId, ex.getMessage());
        }
    }

    private InventorySyncStateEntity synchronizeProperty(String propertyId, String authHeader) {
        List<InventoryRoomEntity> inventoryRooms = inventoryRoomRepository.findAllByPropertyId(propertyId);
        List<RoomOutletTypeEntity> roomTypes = loadRoomTypesOrThrow(propertyId);
        String requestId = newRequestId();

        InventoryReconciliationRequest request = payloadMapper.toRequest(propertyId, roomTypes);

        RuntimeException roomMasterError = null;
        RuntimeException reconcileError = null;

        if (!inventoryRooms.isEmpty()) {
            try {
                syncRoomMaster(propertyId, inventoryRooms, roomTypes, requestId, authHeader);
            } catch (RuntimeException ex) {
                roomMasterError = toSyncException("Room master sync failed", ex);
            }
        }

        try {
            inventorySyncClient.reconcile(request, requestId, authHeader);
        } catch (RuntimeException ex) {
            reconcileError = toSyncException("Inventory reconciliation failed", ex);
        }

        if (roomMasterError == null && reconcileError == null) {
            return markSuccess(propertyId, requestId);
        }

        RuntimeException primary = reconcileError != null ? reconcileError : roomMasterError;
        String failureMessage = buildFailureMessage(roomMasterError, reconcileError);
        throw persistFailureAndWrap(propertyId, requestId, new InventorySyncException(failureMessage, primary));
    }

    private void markPending(String propertyId) {
        updateState(propertyId, InventorySyncStatus.PENDING, null, null, false);
    }

    private void syncRoomMaster(
        String propertyId,
        List<InventoryRoomEntity> rooms,
        List<RoomOutletTypeEntity> roomTypes,
        String requestId,
        String authHeader
    ) {
        RoomMasterSyncRequest roomMasterRequest = payloadMapper.toRoomMasterSyncRequest(propertyId, rooms, roomTypes);
        inventorySyncClient.syncRoomMaster(roomMasterRequest, requestId, authHeader);
    }

    private InventorySyncStateEntity markSuccess(String propertyId, String requestId) {
        return updateState(propertyId, InventorySyncStatus.SUCCESS, requestId, null, true);
    }

    private RuntimeException persistFailureAndWrap(String propertyId, String requestId, RuntimeException ex) {
        updateState(propertyId, InventorySyncStatus.FAILED, requestId, trimError(ex.getMessage()), false);
        return ex instanceof InventorySyncException
            ? (InventorySyncException) ex
            : new InventorySyncException("Inventory sync failed", ex);
    }

    private InventorySyncStateEntity updateState(
        String propertyId,
        InventorySyncStatus status,
        String requestId,
        String error,
        boolean success
    ) {
        InventorySyncStateEntity state = loadOrCreateState(propertyId);

        state.setStatus(status);
        if (requestId != null) {
            state.setLastRequestId(requestId);
        }
        state.setLastError(error);

        int retries = state.getRetryCount() == null ? 0 : state.getRetryCount();
        state.setRetryCount(success ? 0 : retries + 1);
        if (success) {
            state.setLastSyncedAt(Instant.now());
        }

        return syncStateRepository.save(state);
    }

    private InventorySyncStateEntity loadStateOrThrow(String propertyId) {
        return syncStateRepository.findById(propertyId)
            .orElseThrow(() -> new BadRequestException("No sync state found for property: " + propertyId));
    }

    private InventorySyncStateEntity loadOrCreateState(String propertyId) {
        return syncStateRepository.findById(propertyId).orElseGet(() -> {
            InventorySyncStateEntity created = new InventorySyncStateEntity();
            created.setPropertyId(propertyId);
            created.setRetryCount(0);
            return created;
        });
    }

    private List<RoomOutletTypeEntity> loadRoomTypesOrThrow(String propertyId) {
        List<RoomOutletTypeEntity> roomTypes = roomOutletTypeRepository.findAllByPropertyId(propertyId);
        if (roomTypes.isEmpty()) {
            throw new BadRequestException("Cannot sync inventory. No room outlet types found for property: " + propertyId);
        }
        validateRoomTypeKeys(propertyId, roomTypes);
        return roomTypes;
    }

    private void validateRoomTypeKeys(String propertyId, List<RoomOutletTypeEntity> roomTypes) {
        Set<String> seenKeys = new HashSet<>();
        for (RoomOutletTypeEntity roomType : roomTypes) {
            String key = roomTypeSyncKey(roomType);
            if (key.isBlank()) {
                throw new BadRequestException("Cannot sync inventory. Room type key is missing for property: " + propertyId);
            }
            if (!seenKeys.add(key.toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("Cannot sync inventory. Duplicate room type key found: " + key);
            }
        }
    }

    private String roomTypeSyncKey(RoomOutletTypeEntity roomType) {
        String code = roomType.getRoomCode();
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        String name = roomType.getRoomName();
        return name == null ? "" : name.trim();
    }

    private RuntimeException toSyncException(String message, RuntimeException ex) {
        return ex instanceof InventorySyncException
            ? ex
            : new InventorySyncException(message, ex);
    }

    private String buildFailureMessage(RuntimeException roomMasterError, RuntimeException reconcileError) {
        String roomMasterMsg = roomMasterError == null ? null : roomMasterError.getClass().getSimpleName() + ": " + roomMasterError.getMessage();
        String reconcileMsg = reconcileError == null ? null : reconcileError.getClass().getSimpleName() + ": " + reconcileError.getMessage();
        if (roomMasterMsg != null && reconcileMsg != null) {
            return "Room master sync and inventory reconciliation both failed: roomMaster=" + roomMasterMsg + "; reconcile=" + reconcileMsg;
        }
        if (roomMasterMsg != null) {
            return "Room master sync failed: " + roomMasterMsg;
        }
        return "Inventory reconciliation failed: " + reconcileMsg;
    }

    private String newRequestId() {
        return UUID.randomUUID().toString();
    }

    private void registerAfterCommitSync(String propertyId, String authHeader) {
        String capturedPropertyId = propertyId;
        String capturedAuthHeader = authHeader;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                inventorySyncExecutor.execute(() -> syncSilently(capturedPropertyId, capturedAuthHeader));
            }
        });
    }

    private String trimError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    private InventorySyncStatusResponse toStatusResponse(InventorySyncStateEntity state) {
        return new InventorySyncStatusResponse(
            state.getPropertyId(),
            state.getStatus().name(),
            state.getLastRequestId(),
            state.getLastError(),
            state.getRetryCount(),
            state.getLastSyncedAt() == null ? null : state.getLastSyncedAt().toString(),
            state.getUpdatedAt() == null ? null : state.getUpdatedAt().toString()
        );
    }

}


