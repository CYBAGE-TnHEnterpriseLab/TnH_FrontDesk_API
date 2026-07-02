package com.pms.property.domain.room.service;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.room.dto.InventoryRoomRequest;
import com.pms.property.domain.room.dto.InventoryRoomResponse;
import com.pms.property.domain.room.dto.RoomSummaryResponse;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final FloorConfigurationRepository floorConfigurationRepository;
    private final PropertyAreaRepository propertyAreaRepository;
    private final FloorPropertyAreaRepository floorPropertyAreaRepository;
    private final RoomOutletTypeRepository roomOutletTypeRepository;
    private final InventoryRoomRepository inventoryRoomRepository;

    public RoomService(
        FloorConfigurationRepository floorConfigurationRepository,
        PropertyAreaRepository propertyAreaRepository,
        FloorPropertyAreaRepository floorPropertyAreaRepository,
        RoomOutletTypeRepository roomOutletTypeRepository,
        InventoryRoomRepository inventoryRoomRepository
    ) {
        this.floorConfigurationRepository = floorConfigurationRepository;
        this.propertyAreaRepository = propertyAreaRepository;
        this.floorPropertyAreaRepository = floorPropertyAreaRepository;
        this.roomOutletTypeRepository = roomOutletTypeRepository;
        this.inventoryRoomRepository = inventoryRoomRepository;
    }

    @Transactional(readOnly = true)
    public RoomSummaryResponse getSummaryByPropertyId(String propertyId) {
        return new RoomSummaryResponse(
            propertyId,
            floorConfigurationRepository.countByPropertyId(propertyId),
            propertyAreaRepository.countByPropertyId(propertyId),
            floorPropertyAreaRepository.countByPropertyId(propertyId),
            roomOutletTypeRepository.countByPropertyId(propertyId),
            inventoryRoomRepository.countByPropertyId(propertyId)
        );
    }

    @Transactional(readOnly = true)
    public List<InventoryRoomResponse> listInventoryRoomsByPropertyId(String propertyId) {
        return inventoryRoomRepository.findAllByPropertyId(propertyId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryRoomResponse getInventoryRoomById(String propertyId, Long roomId) {
        return inventoryRoomRepository.findByPropertyIdAndId(propertyId, roomId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Inventory room not found: " + roomId));
    }

    @Transactional
    public InventoryRoomResponse createInventoryRoom(String propertyId, InventoryRoomRequest request) {
        InventoryRoomEntity entity = new InventoryRoomEntity();
        entity.setPropertyId(propertyId);
        entity.setFloorName(request.floorName());
        entity.setRoomTypeName(request.roomTypeName());
        entity.setRoomNumber(request.roomNumber());
        return toResponse(inventoryRoomRepository.save(entity));
    }

    @Transactional
    public InventoryRoomResponse updateInventoryRoom(String propertyId, Long roomId, InventoryRoomRequest request) {
        InventoryRoomEntity entity = inventoryRoomRepository.findByPropertyIdAndId(propertyId, roomId)
            .orElseThrow(() -> new NotFoundException("Inventory room not found: " + roomId));
        entity.setFloorName(request.floorName());
        entity.setRoomTypeName(request.roomTypeName());
        entity.setRoomNumber(request.roomNumber());
        return toResponse(inventoryRoomRepository.save(entity));
    }

    @Transactional
    public void deleteInventoryRoom(String propertyId, Long roomId) {
        InventoryRoomEntity entity = inventoryRoomRepository.findByPropertyIdAndId(propertyId, roomId)
            .orElseThrow(() -> new NotFoundException("Inventory room not found: " + roomId));
        inventoryRoomRepository.delete(entity);
    }

    private InventoryRoomResponse toResponse(InventoryRoomEntity entity) {
        return new InventoryRoomResponse(
            entity.getId(),
            entity.getPropertyId(),
            entity.getFloorName(),
            entity.getRoomTypeName(),
            entity.getRoomNumber()
        );
    }
}

