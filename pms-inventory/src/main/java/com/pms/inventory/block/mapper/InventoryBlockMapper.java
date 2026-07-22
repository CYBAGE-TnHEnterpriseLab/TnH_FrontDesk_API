package com.pms.inventory.block.mapper;

import com.pms.inventory.block.dto.response.InventoryBlockResponse;
import com.pms.inventory.block.entity.InventoryBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryBlockMapper {

    @Mapping(target = "blockId", source = "block.id")
    @Mapping(target = "idempotent", source = "idempotent")
    InventoryBlockResponse toResponse(InventoryBlock block, boolean idempotent);
}

