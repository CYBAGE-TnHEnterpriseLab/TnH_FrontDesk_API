package com.pms.property.domain.tax.repository;

import com.pms.property.domain.tax.entity.TaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

	long countByPropertyId(String propertyId);

	long deleteByPropertyId(String propertyId);
}


