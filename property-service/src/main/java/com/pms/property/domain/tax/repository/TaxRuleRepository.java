package com.pms.property.domain.tax.repository;

import com.pms.property.domain.tax.entity.TaxRuleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

	long countByPropertyId(String propertyId);

	List<TaxRuleEntity> findAllByPropertyIdOrderByPriorityAsc(String propertyId);

	Optional<TaxRuleEntity> findByPropertyIdAndId(String propertyId, Long id);

	long deleteByPropertyId(String propertyId);
}


