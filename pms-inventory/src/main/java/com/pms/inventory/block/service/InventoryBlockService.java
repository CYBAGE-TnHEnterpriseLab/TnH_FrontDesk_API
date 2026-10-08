package com.pms.inventory.block.service;

import com.pms.inventory.block.dto.request.CreateInventoryBlockRequest;
import com.pms.inventory.block.dto.response.InventoryBlockResponse;
import com.pms.inventory.block.entity.InventoryBlock;
import com.pms.inventory.block.enums.InventoryBlockStatus;
import com.pms.inventory.block.mapper.InventoryBlockMapper;
import com.pms.inventory.block.repository.InventoryBlockRepository;
import com.pms.inventory.common.exception.InventoryNotFoundException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryBlockService {

    private final InventoryBlockRepository blockRepository;
    private final InventoryService inventoryService;
    private final InventoryBlockMapper blockMapper;

    public InventoryBlockService(
            InventoryBlockRepository blockRepository,
            InventoryService inventoryService,
            InventoryBlockMapper blockMapper
    ) {
        this.blockRepository = blockRepository;
        this.inventoryService = inventoryService;
        this.blockMapper = blockMapper;
    }

    @Transactional
    public InventoryBlockResponse create(CreateInventoryBlockRequest request) {
        List<RoomTypeInventoryDaily> rows = inventoryService.lockInventoryRange(
                request.propertyId(),
                request.roomTypeId(),
                request.fromDate(),
                request.toDate()
        );
        inventoryService.increaseBlocked(rows, request.quantity());

        InventoryBlock block = InventoryBlock.builder()
                .propertyId(request.propertyId())
                .roomTypeId(request.roomTypeId())
                .fromDate(request.fromDate())
                .toDate(request.toDate())
                .quantity(request.quantity())
                .reason(request.reason())
                .status(InventoryBlockStatus.ACTIVE)
                .build();

        return blockMapper.toResponse(blockRepository.save(block), false);
    }

    @Transactional
    public InventoryBlockResponse release(Long blockId) {
        InventoryBlock activeBlock = blockRepository.findByIdAndStatus(blockId, InventoryBlockStatus.ACTIVE).orElse(null);
        if (activeBlock == null) {
            InventoryBlock block = blockRepository.findById(blockId)
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory block not found"));
            return blockMapper.toResponse(block, true);
        }

        List<RoomTypeInventoryDaily> rows = inventoryService.lockInventoryRange(
                activeBlock.getPropertyId(),
                activeBlock.getRoomTypeId(),
                activeBlock.getFromDate(),
                activeBlock.getToDate()
        );
        inventoryService.decreaseBlocked(rows, activeBlock.getQuantity());

        activeBlock.setStatus(InventoryBlockStatus.RELEASED);

        return blockMapper.toResponse(activeBlock, false);
    }
}

