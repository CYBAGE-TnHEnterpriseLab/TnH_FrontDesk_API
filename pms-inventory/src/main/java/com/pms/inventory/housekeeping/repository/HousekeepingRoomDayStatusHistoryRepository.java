package com.pms.inventory.housekeeping.repository;

import com.pms.inventory.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousekeepingRoomDayStatusHistoryRepository extends JpaRepository<HousekeepingRoomDayStatusHistory, Long> {
}

