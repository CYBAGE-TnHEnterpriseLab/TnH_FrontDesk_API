package com.pms.inventory.block.mapper;

import com.pms.inventory.block.dto.response.InventoryBlockResponse;
import com.pms.inventory.block.entity.InventoryBlock;
import com.pms.inventory.block.enums.InventoryBlockStatus;
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
public class InventoryBlockMapperImpl implements InventoryBlockMapper {

    @Override
    public InventoryBlockResponse toResponse(InventoryBlock block, boolean idempotent) {
        if ( block == null ) {
            return null;
        }

        Long blockId = null;
        UUID propertyId = null;
        UUID roomTypeId = null;
        LocalDate fromDate = null;
        LocalDate toDate = null;
        Integer quantity = null;
        String reason = null;
        InventoryBlockStatus status = null;
        if ( block != null ) {
            blockId = block.getId();
            propertyId = block.getPropertyId();
            roomTypeId = block.getRoomTypeId();
            fromDate = block.getFromDate();
            toDate = block.getToDate();
            quantity = block.getQuantity();
            reason = block.getReason();
            status = block.getStatus();
        }
        boolean idempotent1 = false;
        idempotent1 = idempotent;

        InventoryBlockResponse inventoryBlockResponse = new InventoryBlockResponse( blockId, propertyId, roomTypeId, fromDate, toDate, quantity, reason, status, idempotent1 );

        return inventoryBlockResponse;
    }
}
