package com.pms.inventory.inventory.service;

import com.pms.inventory.common.exception.InventoryException;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.repository.RoomTypeInventoryDailyRepository;
import com.pms.inventory.reconciliation.dto.InventoryReconciliationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("inventoryReconciliationDomainService")
public class InventoryReconciliationService {

    private final RoomTypeInventoryDailyRepository dailyRepository;

    public InventoryReconciliationService(RoomTypeInventoryDailyRepository dailyRepository) {
        this.dailyRepository = dailyRepository;
    }

    @Transactional
    public int reconcile(InventoryReconciliationRequest request) {
        validateDateRange(request.fromDate(), request.toDate());

        return request.roomTypes().stream()
                .mapToInt(roomTypeInput -> reconcileRoomType(
                        request.propertyId(),
                        roomTypeInput.roomTypeId(),
                        roomTypeInput.totalInventory(),
                        request.fromDate(),
                        request.toDate()
                ))
                .sum();
    }

    private int reconcileRoomType(
            UUID propertyId,
            UUID roomTypeId,
            int totalInventory,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Map<LocalDate, RoomTypeInventoryDaily> existingByDate = loadExistingByDate(propertyId, roomTypeId, fromDate, toDate);
        List<RoomTypeInventoryDaily> toSave = buildRowsToSave(
                propertyId,
                roomTypeId,
                totalInventory,
                fromDate,
                toDate,
                existingByDate
        );

        dailyRepository.saveAll(toSave);
        return toSave.size();
    }

    private List<RoomTypeInventoryDaily> buildRowsToSave(
            UUID propertyId,
            UUID roomTypeId,
            int totalInventory,
            LocalDate fromDate,
            LocalDate toDate,
            Map<LocalDate, RoomTypeInventoryDaily> existingByDate
    ) {
        List<RoomTypeInventoryDaily> toSave = new ArrayList<>();
        for (LocalDate date = fromDate; date.isBefore(toDate); date = date.plusDays(1)) {
            toSave.add(upsertRowForDate(propertyId, roomTypeId, totalInventory, date, existingByDate));
        }
        return toSave;
    }

    private RoomTypeInventoryDaily upsertRowForDate(
            UUID propertyId,
            UUID roomTypeId,
            int totalInventory,
            LocalDate date,
            Map<LocalDate, RoomTypeInventoryDaily> existingByDate
    ) {
        RoomTypeInventoryDaily existing = existingByDate.get(date);
        return (existing == null)
                ? newDailyRow(propertyId, roomTypeId, date, totalInventory)
                : updateExistingRow(existing, totalInventory, date);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (!toDate.isAfter(fromDate)) {
            throw new InventoryException("Invalid reconciliation date range");
        }
    }

    private Map<LocalDate, RoomTypeInventoryDaily> loadExistingByDate(
            UUID propertyId,
            UUID roomTypeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return dailyRepository.findForUpdate(propertyId, roomTypeId, fromDate, toDate)
                .stream()
                .collect(Collectors.toMap(
                        RoomTypeInventoryDaily::getBusinessDate,
                        Function.identity(),
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private RoomTypeInventoryDaily newDailyRow(
            UUID propertyId,
            UUID roomTypeId,
            LocalDate date,
            int totalInventory
    ) {
        return RoomTypeInventoryDaily.builder()
                .propertyId(propertyId)
                .roomTypeId(roomTypeId)
                .businessDate(date)
                .totalInventory(totalInventory)
                .reservedCount(0)
                .blockedCount(0)
                .build();
    }

    private RoomTypeInventoryDaily updateExistingRow(
            RoomTypeInventoryDaily row,
            int totalInventory,
            LocalDate date
    ) {
        int minimumRequired = row.getReservedCount() + row.getBlockedCount();
        if (totalInventory < minimumRequired) {
            throw new InventoryException("Cannot reduce total inventory below reserved + blocked on " + date);
        }
        row.setTotalInventory(totalInventory);
        return row;
    }
}


