package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.entity.RoomType;
import com.frontdesk.pms.room.exception.BadRequestException;
import com.frontdesk.pms.room.exception.RoomTypeNotFoundException;
import com.frontdesk.pms.room.mapper.RoomTypeMapper;
import com.frontdesk.pms.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository repository;
    private final PropertyValidationService propertyValidationService;

        @Override
        public RoomTypeResponseDTO createRoomType(RoomTypeRequestDTO request) {
        if (request.getPropertyId() == null) {
            throw new BadRequestException("Property ID must be provided in the path.");
        }
        propertyValidationService.assertPropertyExists(request.getPropertyId());

        repository.findByNameAndPropertyId(request.getName(), request.getPropertyId())
            .ifPresent(roomType -> {
                throw new BadRequestException("Room type already exists");
            });

        validateMasterMapping(request);

        RoomType entity = RoomType.builder()
            .name(request.getName())
            .propertyId(request.getPropertyId())
            .isMaster(request.getIsMaster())
            .masterRoomTypeId(request.getMasterRoomTypeId())
            .createdAt(LocalDateTime.now())
            .build();

        RoomType saved = repository.save(entity);
        return RoomTypeResponseDTO.builder()
            .id(saved.getId())
            .name(saved.getName())
            .propertyId(saved.getPropertyId())
            .isMaster(saved.getIsMaster())
            .masterRoomTypeId(saved.getMasterRoomTypeId())
            .build();
        }

    @Override
    public List<RoomTypeResponseDTO> getAllRoomTypes() {
        return repository.findAll()
                .stream()
                .map(RoomTypeMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomTypeResponseDTO> getRoomTypesByPropertyId(UUID propertyId) {
        propertyValidationService.assertPropertyExists(propertyId);
        return repository.findByPropertyId(propertyId)
                .stream()
                .map(RoomTypeMapper::toResponse)
                .toList();
    }

    @Override
    public RoomTypeResponseDTO getRoomTypeById(Long id) {
        RoomType roomType = repository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        return RoomTypeMapper.toResponse(roomType);
    }

    @Override
    public RoomTypeResponseDTO updateRoomType(Long id, RoomTypeRequestDTO request) {
        RoomType existing = repository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        // Use the propertyId from the existing entity, not from the request
        UUID propertyId = existing.getPropertyId();
        propertyValidationService.assertPropertyExists(propertyId);

        repository.findByNameAndPropertyId(request.getName(), propertyId)
                .filter(roomType -> !roomType.getId().equals(id))
                .ifPresent(roomType -> {
                    throw new BadRequestException("Room type name already exists");
                });

        validateMasterMapping(request);

        existing.setName(request.getName());
        // Do NOT update propertyId
        existing.setIsMaster(request.getIsMaster());
        existing.setMasterRoomTypeId(request.getMasterRoomTypeId());

        RoomType updated = repository.save(existing);
        return RoomTypeMapper.toResponse(updated);
    }

    @Override
    public void deleteRoomType(Long id) {
        RoomType existing = repository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        repository.delete(existing);
    }

    private void validateMasterMapping(RoomTypeRequestDTO request) {
        if (Boolean.TRUE.equals(request.getIsMaster())) {
            if (request.getMasterRoomTypeId() != null) {
                throw new BadRequestException("Master room cannot have masterRoomTypeId");
            }
            return;
        }

        // Allow null safely for non-masters
        if (request.getMasterRoomTypeId() == null) {
            return;
        }

        RoomType master = repository.findById(request.getMasterRoomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException("Master room type not found"));

        if (!master.getIsMaster()) {
            throw new BadRequestException("Assigned room is not a master");
        }
        if (!master.getPropertyId().equals(request.getPropertyId())) {
            throw new BadRequestException("Assigned master room type must belong to the same property");
        }
    }

}