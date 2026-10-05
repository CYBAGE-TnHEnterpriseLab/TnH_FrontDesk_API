package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyMemberRepository extends JpaRepository<LoyaltyMember, UUID> {
    Optional<LoyaltyMemberProjection> findMembershipByGuestIdAndLoyaltyProgramId(UUID guestId, UUID loyaltyProgramId);
    Optional<LoyaltyMember> findByLoyaltyNumber(String loyaltyNumber);

    interface LoyaltyMemberProjection {
        UUID getId();
        String getLoyaltyNumber();
        String getTierCode();
        String getTierName();
        Integer getTierLevel();
        String getStatus();
    }
}
