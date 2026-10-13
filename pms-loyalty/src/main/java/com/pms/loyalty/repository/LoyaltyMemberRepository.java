package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyMemberRepository extends JpaRepository<LoyaltyMember, UUID> {
    Optional<LoyaltyMember> findByGuestIdAndLoyaltyProgramId(UUID guestId, UUID loyaltyProgramId);
    Optional<LoyaltyMember> findByLoyaltyNumber(String loyaltyNumber);
}
