package com.pms.property.domain.finance.repository;

import com.pms.property.domain.finance.entity.RevenueMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueMappingRepository extends JpaRepository<RevenueMappingEntity, Long> {

	long countByPropertyId(String propertyId);
}


