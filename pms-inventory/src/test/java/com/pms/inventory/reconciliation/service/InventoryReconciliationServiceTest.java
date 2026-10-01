package com.pms.inventory.reconciliation.service;

import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.repository.RoomTypeInventoryDailyRepository;
import com.pms.inventory.reconciliation.dto.InventoryReconciliationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReconciliationServiceTest {

    @Mock
    private RoomTypeInventoryDailyRepository repository;

    @InjectMocks
    private com.pms.inventory.inventory.service.InventoryReconciliationService service;

    @Test
    void reconciliationCreatesDailyInventoryAndIsIdempotent() {
        String propertyId = "property-1";
        String roomTypeId = "room-type-1";
        InventoryReconciliationRequest request = new InventoryReconciliationRequest(
                propertyId,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 23),
                List.of(new InventoryReconciliationRequest.RoomTypeInventoryInput(roomTypeId, 3))
        );

        when(repository.findForUpdate(propertyId, roomTypeId, request.fromDate(), request.toDate()))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        RoomTypeInventoryDaily.builder().propertyId(propertyId).roomTypeId(roomTypeId).businessDate(LocalDate.of(2026, 7, 20)).totalInventory(3).reservedCount(0).blockedCount(0).build(),
                        RoomTypeInventoryDaily.builder().propertyId(propertyId).roomTypeId(roomTypeId).businessDate(LocalDate.of(2026, 7, 21)).totalInventory(3).reservedCount(0).blockedCount(0).build(),
                        RoomTypeInventoryDaily.builder().propertyId(propertyId).roomTypeId(roomTypeId).businessDate(LocalDate.of(2026, 7, 22)).totalInventory(3).reservedCount(0).blockedCount(0).build()
                ));

        int firstAffected = service.reconcile(request);
        int secondAffected = service.reconcile(request);

        assertEquals(3, firstAffected);
        assertEquals(3, secondAffected);
        verify(repository, times(2)).saveAll(anyList());
    }
}


