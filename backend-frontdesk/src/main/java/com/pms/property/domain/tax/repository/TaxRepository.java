package com.pms.property.domain.tax.repository;

import com.pms.property.domain.tax.entity.TaxEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRepository extends JpaRepository<TaxEntity, Long> {

	Optional<TaxEntity> findByPropertyId(String propertyId);

	Optional<TaxEntity> findByPropertyIdAndId(String propertyId, Long id);
}


