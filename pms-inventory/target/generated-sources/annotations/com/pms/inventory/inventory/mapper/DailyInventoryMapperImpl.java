package com.pms.inventory.inventory.mapper;

import com.pms.inventory.inventory.dto.response.DailyInventoryResponse;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-22T15:28:14+0530",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class DailyInventoryMapperImpl implements DailyInventoryMapper {

    @Override
    public DailyInventoryResponse toResponse(RoomTypeInventoryDaily entity) {
        if ( entity == null ) {
            return null;
        }

        UUID propertyId = null;
        UUID roomTypeId = null;
        LocalDate businessDate = null;
        Integer totalInventory = null;
        Integer reservedCount = null;
        Integer blockedCount = null;

        propertyId = entity.getPropertyId();
        roomTypeId = entity.getRoomTypeId();
        businessDate = entity.getBusinessDate();
        totalInventory = entity.getTotalInventory();
        reservedCount = entity.getReservedCount();
        blockedCount = entity.getBlockedCount();

        Integer availableCount = entity.availableCount();

        DailyInventoryResponse dailyInventoryResponse = new DailyInventoryResponse( propertyId, roomTypeId, businessDate, totalInventory, reservedCount, blockedCount, availableCount );

        return dailyInventoryResponse;
    }
}
