package com.pms.inventory.block.service;

import com.pms.inventory.block.dto.request.CreateInventoryBlockRequest;
import com.pms.inventory.block.dto.response.InventoryBlockResponse;
import com.pms.inventory.block.entity.InventoryBlock;
import com.pms.inventory.block.enums.InventoryBlockStatus;
import com.pms.inventory.block.mapper.InventoryBlockMapper;
import com.pms.inventory.block.repository.InventoryBlockRepository;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryBlockServiceTest {

    @Mock
    private InventoryBlockRepository blockRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InventoryBlockMapper mapper;

    @InjectMocks
    private InventoryBlockService service;

    @Test
    void inventoryBlockCreation() {
        CreateInventoryBlockRequest request = new CreateInventoryBlockRequest(
                "property-1", "room-type-1", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22), 1, "Wedding"
        );
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());
        InventoryBlock block = InventoryBlock.builder().id(1L).status(InventoryBlockStatus.ACTIVE).build();
        InventoryBlockResponse expected = new InventoryBlockResponse(1L, request.propertyId(), request.roomTypeId(),
                request.fromDate(), request.toDate(), request.quantity(), request.reason(), InventoryBlockStatus.ACTIVE, false);

        when(inventoryService.lockInventoryRange(any(), any(), any(), any())).thenReturn(rows);
        when(blockRepository.save(any(InventoryBlock.class))).thenReturn(block);
        when(mapper.toResponse(block, false)).thenReturn(expected);

        InventoryBlockResponse response = service.create(request);

        assertEquals(expected, response);
        verify(inventoryService).increaseBlocked(rows, 1);
    }

    @Test
    void inventoryBlockReleaseAndDuplicateReleaseIdempotent() {
        InventoryBlock block = InventoryBlock.builder()
                .id(10L)
                .propertyId("property-1")
                .roomTypeId("room-type-1")
                .fromDate(LocalDate.of(2026, 7, 20))
                .toDate(LocalDate.of(2026, 7, 21))
                .quantity(1)
                .status(InventoryBlockStatus.ACTIVE)
                .build();
        List<RoomTypeInventoryDaily> rows = List.of(new RoomTypeInventoryDaily());

        InventoryBlockResponse released = new InventoryBlockResponse(10L, block.getPropertyId(), block.getRoomTypeId(),
                block.getFromDate(), block.getToDate(), block.getQuantity(), "", InventoryBlockStatus.RELEASED, false);

        when(blockRepository.findByIdAndStatus(10L, InventoryBlockStatus.ACTIVE)).thenReturn(Optional.of(block));
        when(inventoryService.lockInventoryRange(any(), any(), any(), any())).thenReturn(rows);
        when(mapper.toResponse(block, false)).thenReturn(released);

        InventoryBlockResponse response = service.release(10L);

        assertEquals(InventoryBlockStatus.RELEASED, block.getStatus());
        assertEquals(released, response);
        verify(inventoryService).decreaseBlocked(rows, 1);

        InventoryBlockResponse idempotent = new InventoryBlockResponse(10L, block.getPropertyId(), block.getRoomTypeId(),
                block.getFromDate(), block.getToDate(), block.getQuantity(), "", InventoryBlockStatus.RELEASED, true);
        when(blockRepository.findByIdAndStatus(10L, InventoryBlockStatus.ACTIVE)).thenReturn(Optional.empty());
        when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        when(mapper.toResponse(block, true)).thenReturn(idempotent);

        InventoryBlockResponse secondResponse = service.release(10L);
        assertEquals(true, secondResponse.idempotent());
        verify(inventoryService, never()).increaseBlocked(rows, 1);
    }
}

