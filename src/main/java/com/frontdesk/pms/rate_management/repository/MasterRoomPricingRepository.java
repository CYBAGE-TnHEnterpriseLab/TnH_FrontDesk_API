package com.frontdesk.pms.rate_management.repository;

import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterRoomPricingRepository extends JpaRepository<MasterRoomPricing, Long> {
}