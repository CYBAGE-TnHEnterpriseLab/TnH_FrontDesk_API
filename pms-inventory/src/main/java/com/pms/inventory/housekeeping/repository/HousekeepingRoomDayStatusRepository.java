package com.pms.inventory.housekeeping.repository;

import com.pms.inventory.housekeeping.entity.CleaningStatus;
import com.pms.inventory.housekeeping.entity.FrontOfficeStatus;
import com.pms.inventory.housekeeping.entity.HousekeepingRoomDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HousekeepingRoomDayStatusRepository extends JpaRepository<HousekeepingRoomDayStatus, Long> {

    List<HousekeepingRoomDayStatus> findAllByPropertyIdAndBusinessDate(UUID propertyId, LocalDate businessDate);

    Optional<HousekeepingRoomDayStatus> findByPropertyIdAndBusinessDateAndRoomNumber(UUID propertyId, LocalDate businessDate, String roomNumber);

    List<HousekeepingRoomDayStatus> findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndAssignedReservationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
            UUID propertyId,
            LocalDate businessDate,
            UUID roomTypeId,
            List<CleaningStatus> cleaningStatuses,
            FrontOfficeStatus frontOfficeStatus
    );
}

