package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.entity.RoomType;
import com.frontdesk.pms.room.exception.RoomTypeNotFoundException;
import com.frontdesk.pms.room.mapper.RoomTypeMapper;
import com.frontdesk.pms.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository repository;

    @Override
    public RoomTypeResponseDTO createRoomType(RoomTypeRequestDTO request) {

        // 🔥 Rule 1: Name must be unique
        repository.findByName(request.getName())
                .ifPresent(r -> {
                    throw new RuntimeException("Room type already exists");
                });

        // 🔥 Rule 2: Master logic
        if (request.getIsMaster()) {
            if (request.getMasterRoomTypeId() != null) {
                throw new RuntimeException("Master room cannot have masterRoomTypeId");
            }
        } else {
            if (request.getMasterRoomTypeId() == null) {
                throw new RuntimeException("Non-master must have masterRoomTypeId");
            }

            // 🔥 Rule 3: Master must exist
            RoomType master = repository.findById(request.getMasterRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("Master room type not found"));

            if (!master.getIsMaster()) {
                throw new RuntimeException("Assigned room is not a master");
            }
        }

        // Convert DTO → Entity
        RoomType entity = RoomType.builder()
                .name(request.getName())
                .propertyId(request.getPropertyId())
                .isMaster(request.getIsMaster())
                .masterRoomTypeId(request.getMasterRoomTypeId())
                .createdAt(LocalDateTime.now())
                .build();

        // Save
        RoomType saved = repository.save(entity);

        // Convert → Response
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
    public RoomTypeResponseDTO getRoomTypeById(Long id) {
        RoomType roomType = repository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        return RoomTypeMapper.toResponse(roomType);
    }

    @Override
    public RoomTypeResponseDTO updateRoomType(Long id, RoomTypeRequestDTO request) {

        // Check existing
        RoomType existing = repository.findById(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        // Check unique name
        repository.findByName(request.getName())
                .filter(rt -> !rt.getId().equals(id))
                .ifPresent(rt -> {
                    throw new RuntimeException("Room type name already exists");
                });

        // Master logic
        if (request.getIsMaster()) {
            if (request.getMasterRoomTypeId() != null) {
                throw new RuntimeException("Master cannot have masterRoomTypeId");
            }
        } else {
            if (request.getMasterRoomTypeId() == null) {
                throw new RuntimeException("Non-master must have masterRoomTypeId");
            }

            RoomType master = repository.findById(request.getMasterRoomTypeId())
                    .orElseThrow(() -> new RoomTypeNotFoundException("Master room type not found"));

            if (!master.getIsMaster()) {
                throw new RuntimeException("Assigned room is not master");
            }
        }

        // Update fields
        existing.setName(request.getName());
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
}