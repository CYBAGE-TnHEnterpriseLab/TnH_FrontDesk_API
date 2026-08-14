package com.pms.inventory.inventory.service;

import com.pms.inventory.common.exception.InsufficientInventoryException;
import com.pms.inventory.common.exception.InventoryNotFoundException;
import com.pms.inventory.inventory.dto.response.DailyInventoryResponse;
import com.pms.inventory.inventory.dto.response.PropertyDeletionCheckResponse;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.mapper.DailyInventoryMapper;
import com.pms.inventory.inventory.repository.RoomTypeInventoryDailyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private final RoomTypeInventoryDailyRepository dailyRepository;
    private final DailyInventoryMapper dailyInventoryMapper;

    public InventoryService(
            RoomTypeInventoryDailyRepository dailyRepository,
            DailyInventoryMapper dailyInventoryMapper
    ) {
        this.dailyRepository = dailyRepository;
        this.dailyInventoryMapper = dailyInventoryMapper;
    }

    @Transactional(readOnly = true)
    public DailyInventoryResponse getDailyInventory(UUID propertyId, UUID roomTypeId, LocalDate businessDate) {
        RoomTypeInventoryDaily daily = dailyRepository
                .findByPropertyIdAndRoomTypeIdAndBusinessDate(propertyId, roomTypeId, businessDate)
                .orElseThrow(() -> new InventoryNotFoundException("Daily inventory not found"));
        return dailyInventoryMapper.toResponse(daily);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeInventoryDaily> getInventoryRange(UUID propertyId, UUID roomTypeId, LocalDate fromDate, LocalDate toDate) {
        return dailyRepository
                .findByPropertyIdAndRoomTypeIdAndBusinessDateGreaterThanEqualAndBusinessDateLessThanOrderByBusinessDate(
                        propertyId,
                        roomTypeId,
                        fromDate,
                        toDate
                );
    }

    @Transactional
    public List<RoomTypeInventoryDaily> lockInventoryRange(UUID propertyId, UUID roomTypeId, LocalDate fromDate, LocalDate toDate) {
        List<RoomTypeInventoryDaily> rows = dailyRepository.findForUpdate(propertyId, roomTypeId, fromDate, toDate);
        long expectedDays = fromDate.datesUntil(toDate).count();
        if (rows.size() != expectedDays) {
            throw new InventoryNotFoundException("Inventory rows not available for full stay date range");
        }
        return rows;
    }

    public void ensureSufficientInventory(List<RoomTypeInventoryDaily> rows, int quantity) {
        for (RoomTypeInventoryDaily daily : rows) {
            if (availableCount(daily) < quantity) {
                throw new InsufficientInventoryException(
                        "Insufficient inventory available for " + daily.getBusinessDate()
                );
            }
        }
    }

    public void increaseReserved(List<RoomTypeInventoryDaily> rows, int quantity) {
        for (RoomTypeInventoryDaily daily : rows) {
            daily.setReservedCount(daily.getReservedCount() + quantity);
        }
    }

    public void decreaseReserved(List<RoomTypeInventoryDaily> rows, int quantity) {
        for (RoomTypeInventoryDaily daily : rows) {
            int updated = daily.getReservedCount() - quantity;
            if (updated < 0) {
                throw new InsufficientInventoryException("Reserved count cannot become negative");
            }
            daily.setReservedCount(updated);
        }
    }

    public void increaseBlocked(List<RoomTypeInventoryDaily> rows, int quantity) {
        for (RoomTypeInventoryDaily daily : rows) {
            if (availableCount(daily) < quantity) {
                throw new InsufficientInventoryException(
                        "Insufficient inventory available to block for " + daily.getBusinessDate()
                );
            }
            daily.setBlockedCount(daily.getBlockedCount() + quantity);
        }
    }

    public void decreaseBlocked(List<RoomTypeInventoryDaily> rows, int quantity) {
        for (RoomTypeInventoryDaily daily : rows) {
            int updated = daily.getBlockedCount() - quantity;
            if (updated < 0) {
                throw new InsufficientInventoryException("Blocked count cannot become negative");
            }
            daily.setBlockedCount(updated);
        }
    }

    public int availableCount(RoomTypeInventoryDaily row) {
        return row.getTotalInventory() - row.getReservedCount() - row.getBlockedCount();
    }

    public Map<LocalDate, RoomTypeInventoryDaily> toDateMap(List<RoomTypeInventoryDaily> rows) {
        Map<LocalDate, RoomTypeInventoryDaily> map = new HashMap<>();
        for (RoomTypeInventoryDaily row : rows) {
            map.put(row.getBusinessDate(), row);
        }
        return map;
    }

    public PropertyDeletionCheckResponse hasAnyActiveReservations(
            UUID propertyId,
            LocalDate businessDate,
            Integer reservedCount
    ) {

        boolean hasActiveReservations =
                dailyRepository.existsByPropertyIdAndBusinessDateGreaterThanEqualAndReservedCountGreaterThan(
                        propertyId,
                        businessDate,
                        reservedCount
                );

        boolean canDelete = !hasActiveReservations;

        return new PropertyDeletionCheckResponse(
                propertyId,
                canDelete,
                hasActiveReservations
        );
    }
}

