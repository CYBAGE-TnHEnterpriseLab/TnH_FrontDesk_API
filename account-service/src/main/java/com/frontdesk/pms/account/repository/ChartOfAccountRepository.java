package com.frontdesk.pms.account.repository;

import com.frontdesk.pms.account.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {

    List<ChartOfAccount> findByPropertyId(UUID propertyId);

    Optional<ChartOfAccount> findByIdAndPropertyId(UUID id, UUID propertyId);

    boolean existsByPropertyIdAndCodeIgnoreCase(UUID propertyId, String code);

    boolean existsByPropertyIdAndNameIgnoreCase(UUID propertyId, String name);
}
