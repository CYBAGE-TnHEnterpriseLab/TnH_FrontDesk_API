package com.pms.property.domain.content.repository;

import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyOverviewRepository extends JpaRepository<PropertyOverviewEntity, Long> {

	List<PropertyOverviewEntity> findAllByPropertyId(String propertyId);

	Optional<PropertyOverviewEntity> findByPropertyId(String propertyId);

	Optional<PropertyOverviewEntity> findByPropertyIdAndId(String propertyId, Long id);
}


