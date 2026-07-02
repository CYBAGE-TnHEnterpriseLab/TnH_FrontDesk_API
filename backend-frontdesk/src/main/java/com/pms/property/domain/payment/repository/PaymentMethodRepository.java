package com.pms.property.domain.payment.repository;

import com.pms.property.domain.payment.entity.PaymentMethodEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

	List<PaymentMethodEntity> findAllByPropertyId(String propertyId);

	Optional<PaymentMethodEntity> findByPropertyIdAndId(String propertyId, Long id);

	long countByPropertyId(String propertyId);
}


