package com.pms.loyalty.repository;

import com.pms.loyalty.entity.LoyaltyProgram;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, UUID> {
}
