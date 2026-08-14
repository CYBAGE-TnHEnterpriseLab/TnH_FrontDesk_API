package com.pms.inventory.inventory.repository;

import com.pms.inventory.inventory.dto.response.PropertyDeletionCheckResponse;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomTypeInventoryDailyRepository extends JpaRepository<RoomTypeInventoryDaily, Long> {

	Optional<RoomTypeInventoryDaily> findByPropertyIdAndRoomTypeIdAndBusinessDate(
			UUID propertyId,
			UUID roomTypeId,
			LocalDate businessDate
	);

	List<RoomTypeInventoryDaily> findByPropertyIdAndRoomTypeIdAndBusinessDateGreaterThanEqualAndBusinessDateLessThanOrderByBusinessDate(
			UUID propertyId,
			UUID roomTypeId,
			LocalDate fromDate,
			LocalDate toDate
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT i FROM RoomTypeInventoryDaily i
			WHERE i.propertyId = :propertyId
			  AND i.roomTypeId = :roomTypeId
			  AND i.businessDate >= :fromDate
			  AND i.businessDate < :toDate
			ORDER BY i.businessDate
			""")
	List<RoomTypeInventoryDaily> findForUpdate(
			@Param("propertyId") UUID propertyId,
			@Param("roomTypeId") UUID roomTypeId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate
	);

	boolean existsByPropertyIdAndBusinessDateGreaterThanEqualAndReservedCountGreaterThan(
			UUID propertyId,
			LocalDate businessDate,
			Integer reservedCount
	);
}

