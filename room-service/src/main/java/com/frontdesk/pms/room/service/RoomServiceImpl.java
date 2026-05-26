package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomRequestDTO;
import com.frontdesk.pms.room.dto.RoomResponseDTO;
import com.frontdesk.pms.room.entity.Floor;
import com.frontdesk.pms.room.entity.Room;
import com.frontdesk.pms.room.entity.RoomType;
import com.frontdesk.pms.room.exception.BadRequestException;
import com.frontdesk.pms.room.exception.FloorNotFoundException;
import com.frontdesk.pms.room.exception.RoomNotFoundException;
import com.frontdesk.pms.room.exception.RoomTypeNotFoundException;
import com.frontdesk.pms.room.mapper.RoomMapper;
import com.frontdesk.pms.room.repository.FloorRepository;
import com.frontdesk.pms.room.repository.RoomRepository;
import com.frontdesk.pms.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository repository;
    private final FloorRepository floorRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyValidationService propertyValidationService;

    @Override
    public List<RoomResponseDTO> createRooms(RoomRequestDTO request) {
        propertyValidationService.assertPropertyExists(request.getPropertyId());

        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new FloorNotFoundException("Floor not found"));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        if (!floor.getPropertyId().equals(request.getPropertyId())) {
            throw new BadRequestException("Floor does not belong to the requested property");
        }
        if (!roomType.getPropertyId().equals(request.getPropertyId())) {
            throw new BadRequestException("Room type does not belong to the requested property");
        }

        List<Room> existingRooms = repository.findByFloorId(request.getFloorId());
        int startNumber = existingRooms.size() + 1;

        List<Room> roomsToSave = new ArrayList<>();
        for (int i = 0; i < request.getNumberOfRooms(); i++) {
            int roomSequence = startNumber + i;
            String roomNumber = floor.getFloorNumber() + String.format("%02d", roomSequence);

            Room room = Room.builder()
                    .roomNumber(roomNumber)
                    .floorId(request.getFloorId())
                    .roomTypeId(request.getRoomTypeId())
                    .propertyId(request.getPropertyId())
                    .createdAt(LocalDateTime.now())
                    .build();

            roomsToSave.add(room);
        }

        List<Room> savedRooms = repository.saveAll(roomsToSave);

        return savedRooms.stream()
                .map(room -> RoomResponseDTO.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .floorId(room.getFloorId())
                        .roomTypeId(room.getRoomTypeId())
                        .build())
                .toList();
    }

    @Override
    public List<RoomResponseDTO> getAllRooms() {
        return repository.findAll()
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomResponseDTO> getRoomsByFloor(Long floorId) {
        return repository.findByFloorId(floorId)
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomResponseDTO> getRoomsByFloorAndPropertyId(Long floorId, UUID propertyId) {
        propertyValidationService.assertPropertyExists(propertyId);
        return repository.findByFloorIdAndPropertyId(floorId, propertyId)
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomResponseDTO> getRoomsByProperty(UUID propertyId) {
        propertyValidationService.assertPropertyExists(propertyId);
        return repository.findByPropertyId(propertyId)
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomResponseDTO> getRoomsByType(Long roomTypeId) {
        return repository.findByRoomTypeId(roomTypeId)
                .stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    @Override
    public RoomResponseDTO updateRoom(Long roomId, Long roomTypeId) {
        Room room = repository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException("Room type not found"));

        if (!room.getPropertyId().equals(roomType.getPropertyId())) {
            throw new BadRequestException("RoomType does not belong to the same property as Room");
        }

        room.setRoomTypeId(roomTypeId);
        Room updated = repository.save(room);

        return RoomMapper.toResponse(updated);
    }

    @Override
    public void deleteRoom(Long roomId) {
        Room room = repository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        repository.delete(room);
    }
}
