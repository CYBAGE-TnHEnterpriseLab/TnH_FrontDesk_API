package com.pms.inventory.inventory.service;

import com.pms.inventory.common.exception.InsufficientInventoryException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.mapper.DailyInventoryMapper;
import com.pms.inventory.inventory.repository.RoomTypeInventoryDailyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {

    private final InventoryService service = new InventoryService(
            Mockito.mock(RoomTypeInventoryDailyRepository.class),
            Mockito.mock(DailyInventoryMapper.class)
    );

    @Test
    void reservedCountNeverBecomesNegative() {
        RoomTypeInventoryDaily row = RoomTypeInventoryDaily.builder()
                .totalInventory(3)
                .reservedCount(0)
                .blockedCount(0)
                .build();

        assertThrows(InsufficientInventoryException.class, () -> service.decreaseReserved(List.of(row), 1));
    }

    @Test
    void blockedCountNeverBecomesNegative() {
        RoomTypeInventoryDaily row = RoomTypeInventoryDaily.builder()
                .totalInventory(3)
                .reservedCount(0)
                .blockedCount(0)
                .build();

        assertThrows(InsufficientInventoryException.class, () -> service.decreaseBlocked(List.of(row), 1));
    }
}

