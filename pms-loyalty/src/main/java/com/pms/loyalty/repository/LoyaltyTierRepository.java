package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyTier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, UUID> {
    List<LoyaltyTier> findByLoyaltyProgramIdAndActiveTrue(UUID loyaltyProgramId);
    Optional<LoyaltyTier> findByLoyaltyProgramIdAndTierCode(UUID loyaltyProgramId, String tierCode);
}
