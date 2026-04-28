package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.FloorRequestDTO;
import com.frontdesk.pms.room.dto.FloorResponseDTO;
import com.frontdesk.pms.room.entity.Floor;
import com.frontdesk.pms.room.exception.FloorNotFoundException;
import com.frontdesk.pms.room.mapper.FloorMapper;
import com.frontdesk.pms.room.repository.FloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FloorServiceImpl implements FloorService {

    private final FloorRepository repository;

    @Override
    public FloorResponseDTO createFloor(FloorRequestDTO request) {

        Floor floor = Floor.builder()
        .name(request.getName())
        .propertyId(request.getPropertyId())
        .floorNumber(request.getFloorNumber())   
        .createdAt(LocalDateTime.now())
        .build();

        Floor saved = repository.save(floor);

        return FloorResponseDTO.builder()
            .id(saved.getId())
            .name(saved.getName())
            .propertyId(saved.getPropertyId())
            .floorNumber(saved.getFloorNumber())   
            .build();
    }

    @Override
    public List<FloorResponseDTO> getAllFloors() {
        return repository.findAll()
                .stream()
                .map(FloorMapper::toResponse)
                .toList();
    }

    @Override
    public FloorResponseDTO getFloorById(Long id) {
        Floor floor = repository.findById(id)
                .orElseThrow(() -> new FloorNotFoundException("Floor not found"));

        return FloorMapper.toResponse(floor);
    }

    @Override
    public FloorResponseDTO updateFloor(Long id, FloorRequestDTO request) {

        Floor existing = repository.findById(id)
                .orElseThrow(() -> new FloorNotFoundException("Floor not found"));

        existing.setName(request.getName());
        existing.setFloorNumber(request.getFloorNumber());

        Floor updated = repository.save(existing);

        return FloorMapper.toResponse(updated);
    }

    @Override
    public void deleteFloor(Long id) {

        Floor existing = repository.findById(id)
                .orElseThrow(() -> new FloorNotFoundException("Floor not found"));

        repository.delete(existing);
    }


}