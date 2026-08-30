package com.pms.property.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfigEntity, Long> {
}

