package com.pms.property.domain.content.repository;

import com.pms.property.domain.content.entity.NearbyLocationAccessibilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NearbyLocationAccessibilityRepository extends JpaRepository<NearbyLocationAccessibilityEntity, Long> {

	long countByPropertyId(String propertyId);
}


