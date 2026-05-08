package com.frontdesk.pms.account.repository;

import com.frontdesk.pms.account.enums.ChargeType;
import com.frontdesk.pms.account.entity.RevenueMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevenueMappingRepository extends JpaRepository<RevenueMapping, UUID> {

    List<RevenueMapping> findByPropertyId(UUID propertyId);

    Optional<RevenueMapping> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByPropertyIdAndChargeType(UUID propertyId, ChargeType chargeType);
}
