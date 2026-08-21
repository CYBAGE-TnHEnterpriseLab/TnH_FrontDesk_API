package com.pms.housekeeping.repository;

import com.pms.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface HousekeepingRoomDayStatusHistoryRepository extends JpaRepository<HousekeepingRoomDayStatusHistory, Long> {

    Optional<HousekeepingRoomDayStatusHistory>
    findFirstByPropertyIdAndRoomNumberAndBusinessDateLessThanEqualAndChangedFieldOrderByBusinessDateDescChangedAtDesc(
            UUID propertyId,
            String roomNumber,
            LocalDate businessDate,
            String changedField
    );

}


