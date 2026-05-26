package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.FloorRequestDTO;
import com.frontdesk.pms.room.dto.FloorResponseDTO;
import java.util.List;

public interface FloorService {

    FloorResponseDTO createFloor(FloorRequestDTO request);

    List<FloorResponseDTO> getAllFloors();

    List<FloorResponseDTO> getFloorsByPropertyId(java.util.UUID propertyId);

    FloorResponseDTO getFloorById(Long id);

    FloorResponseDTO updateFloor(Long id, FloorRequestDTO request);

    void deleteFloor(Long id);
}