package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.FloorRequestDTO;
import com.frontdesk.pms.room.dto.FloorResponseDTO;
import com.frontdesk.pms.room.entity.Floor;
import com.frontdesk.pms.room.exception.FloorNotFoundException;
import com.frontdesk.pms.room.repository.FloorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorServiceImplTest {

    @Mock
    private FloorRepository repository;

    @Mock
    private PropertyValidationService propertyValidationService;

    @InjectMocks
    private FloorServiceImpl service;

    @Test
    void createFloorValidatesPropertyAndSaves() {
        UUID propertyId = UUID.randomUUID();
        FloorRequestDTO request = floorRequest(propertyId);
        when(repository.save(org.mockito.ArgumentMatchers.any(Floor.class)))
                .thenAnswer(invocation -> {
                    Floor floor = invocation.getArgument(0);
                    floor.setId(1L);
                    return floor;
                });

        FloorResponseDTO response = service.createFloor(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getFloorNumber()).isEqualTo(5);
        verify(propertyValidationService).assertPropertyExists(propertyId);
    }

    @Test
    void getFloorByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFloorById(99L))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void updateFloorChangesExistingFloor() {
        UUID propertyId = UUID.randomUUID();
        Floor existing = floor(1L, UUID.randomUUID());
        FloorRequestDTO request = floorRequest(propertyId);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FloorResponseDTO response = service.updateFloor(1L, request);

        assertThat(response.getName()).isEqualTo("Fifth Floor");
        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        verify(propertyValidationService).assertPropertyExists(propertyId);
    }

    @Test
    void getFloorsByPropertyIdValidatesAndMapsResults() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findByPropertyId(propertyId)).thenReturn(List.of(floor(1L, propertyId)));

        List<FloorResponseDTO> results = service.getFloorsByPropertyId(propertyId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPropertyId()).isEqualTo(propertyId);
        verify(propertyValidationService).assertPropertyExists(propertyId);
    }

    private FloorRequestDTO floorRequest(UUID propertyId) {
        FloorRequestDTO request = new FloorRequestDTO();
        request.setName("Fifth Floor");
        request.setPropertyId(propertyId);
        request.setFloorNumber(5);
        return request;
    }

    private Floor floor(Long id, UUID propertyId) {
        return Floor.builder()
                .id(id)
                .name("Old Floor")
                .propertyId(propertyId)
                .floorNumber(2)
                .build();
    }
}
