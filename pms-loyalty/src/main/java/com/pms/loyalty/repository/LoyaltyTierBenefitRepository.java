package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyTierBenefit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTierBenefitRepository extends JpaRepository<LoyaltyTierBenefit, UUID> {
    List<LoyaltyTierBenefit> findByTierId(UUID tierId);
}
