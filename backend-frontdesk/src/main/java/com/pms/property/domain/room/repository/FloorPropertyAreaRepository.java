package com.pms.property.domain.room.repository;

import com.pms.property.domain.room.entity.FloorPropertyAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorPropertyAreaRepository extends JpaRepository<FloorPropertyAreaEntity, Long> {

	long countByPropertyId(String propertyId);

	long deleteByPropertyId(String propertyId);
}


