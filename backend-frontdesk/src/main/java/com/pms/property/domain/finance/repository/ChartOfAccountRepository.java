package com.pms.property.domain.finance.repository;

import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccountEntity, Long> {

	List<ChartOfAccountEntity> findAllByPropertyId(String propertyId);

	Optional<ChartOfAccountEntity> findByPropertyIdAndId(String propertyId, Long id);

	long countByPropertyId(String propertyId);
}


