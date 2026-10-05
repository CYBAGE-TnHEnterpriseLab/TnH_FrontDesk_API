package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyBenefit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyBenefitRepository extends JpaRepository<LoyaltyBenefit, UUID> {
    Optional<LoyaltyBenefit> findByBenefitCode(String benefitCode);
}
