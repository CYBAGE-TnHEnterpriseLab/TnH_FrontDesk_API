package com.pms.property.domain.room.repository;

import com.pms.property.domain.room.entity.InventoryRoomEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRoomRepository extends JpaRepository<InventoryRoomEntity, Long> {

	List<InventoryRoomEntity> findAllByPropertyId(String propertyId);

	Optional<InventoryRoomEntity> findByPropertyIdAndId(String propertyId, Long id);

	long countByPropertyId(String propertyId);

	long deleteByPropertyId(String propertyId);
}


