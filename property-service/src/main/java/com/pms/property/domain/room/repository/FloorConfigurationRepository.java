package com.pms.property.domain.room.repository;

import com.pms.property.domain.room.entity.FloorConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorConfigurationRepository extends JpaRepository<FloorConfigurationEntity, Long> {

	long countByPropertyId(String propertyId);

	long deleteByPropertyId(String propertyId);
}


