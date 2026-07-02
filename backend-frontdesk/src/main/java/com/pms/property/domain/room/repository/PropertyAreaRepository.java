package com.pms.property.domain.room.repository;

import com.pms.property.domain.room.entity.PropertyAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyAreaRepository extends JpaRepository<PropertyAreaEntity, Long> {

	long countByPropertyId(String propertyId);
}


