package com.frontdesk.pms.rate_management.repository;

import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterRoomPricingRepository extends JpaRepository<MasterRoomPricing, Long> {
	List<MasterRoomPricing> findByMasterRoomId(Long masterRoomId);

	List<MasterRoomPricing> findByRoomTypeId(Long roomTypeId);

	Optional<MasterRoomPricing> findByMasterRoomIdAndOccupancyType(Long masterRoomId, String occupancyType);

	Optional<MasterRoomPricing> findByRoomTypeIdAndOccupancyType(Long roomTypeId, String occupancyType);

	List<MasterRoomPricing> findByInheritedTrueAndParentPricingId(Long parentPricingId);
}