package com.pms.inventory.inventory.mapper;

import com.pms.inventory.inventory.dto.response.DailyInventoryResponse;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DailyInventoryMapper {

    @Mapping(target = "availableCount", expression = "java(entity.availableCount())")
    DailyInventoryResponse toResponse(RoomTypeInventoryDaily entity);
}

