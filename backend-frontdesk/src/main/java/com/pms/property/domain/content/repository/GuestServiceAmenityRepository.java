package com.pms.property.domain.content.repository;

import com.pms.property.domain.content.entity.GuestServiceAmenityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestServiceAmenityRepository extends JpaRepository<GuestServiceAmenityEntity, Long> {

	long countByPropertyId(String propertyId);
}


