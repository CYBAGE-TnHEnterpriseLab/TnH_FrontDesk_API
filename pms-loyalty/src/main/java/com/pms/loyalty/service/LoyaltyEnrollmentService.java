package com.pms.loyalty.service;

import com.pms.loyalty.dto.response.LoyaltyMembershipResponse;
import com.pms.loyalty.entity.LoyaltyMember;
import com.pms.loyalty.entity.LoyaltyTier;
import com.pms.loyalty.exception.LoyaltyConfigurationException;
import com.pms.loyalty.repository.LoyaltyMemberRepository;
import com.pms.loyalty.repository.LoyaltyTierRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoyaltyEnrollmentService {

    private final LoyaltyTierRepository loyaltyTierRepository;
    private final LoyaltyMemberRepository loyaltyMemberRepository;
    private final LoyaltyNumberGenerator loyaltyNumberGenerator;
    private final Random random = new Random();

    @Transactional
    public LoyaltyMembershipResponse enroll(UUID guestId, UUID loyaltyProgramId) {
        List<LoyaltyTier> activeTiers = loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(loyaltyProgramId);
        if (activeTiers.isEmpty()) {
            throw new LoyaltyConfigurationException(
                    "No active loyalty tiers configured for loyaltyProgramId=" + loyaltyProgramId);
        }

        LoyaltyTier randomTier = activeTiers.get(random.nextInt(activeTiers.size()));

        String loyaltyNumber = loyaltyNumberGenerator.generateLoyaltyNumber();
        LocalDateTime now = LocalDateTime.now();

        LoyaltyMember member = LoyaltyMember.builder()
                .id(UUID.randomUUID())
                .loyaltyProgramId(loyaltyProgramId)
                .guestId(guestId)
                .loyaltyNumber(loyaltyNumber)
                .currentTierId(randomTier.getId())
                .status("ACTIVE")
                .enrollmentSource("BOOKING")
                .enrolledAt(now)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        LoyaltyMember saved = loyaltyMemberRepository.save(member);

        return toResponse(saved, randomTier, true);
    }

    @Transactional
    public LoyaltyMembershipResponse ensureMembership(UUID guestId, UUID loyaltyProgramId) {
        List<LoyaltyTier> activeTiers = loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(loyaltyProgramId);
        if (activeTiers.isEmpty()) {
            throw new LoyaltyConfigurationException(
                    "No active loyalty tiers configured for loyaltyProgramId=" + loyaltyProgramId);
        }

        LoyaltyTier randomTier = activeTiers.get(random.nextInt(activeTiers.size()));

        LoyaltyMemberRepository.LoyaltyMemberProjection existing = loyaltyMemberRepository
                .findMembershipByGuestIdAndLoyaltyProgramId(guestId, loyaltyProgramId)
                .orElse(null);

        if (existing != null) {
            return LoyaltyMembershipResponse.builder()
                    .membershipId(existing.getId())
                    .guestId(guestId)
                    .loyaltyNumber(existing.getLoyaltyNumber())
                    .tierCode(existing.getTierCode())
                    .tierName(existing.getTierName())
                    .tierLevel(existing.getTierLevel())
                    .status(existing.getStatus())
                    .newlyEnrolled(false)
                    .build();
        }

        return enroll(guestId, loyaltyProgramId);
    }

    private LoyaltyMembershipResponse toResponse(LoyaltyMember member, LoyaltyTier tier, boolean newlyEnrolled) {
        return LoyaltyMembershipResponse.builder()
                .membershipId(member.getId())
                .guestId(member.getGuestId())
                .loyaltyNumber(member.getLoyaltyNumber())
                .tierCode(tier.getTierCode())
                .tierName(tier.getTierName())
                .tierLevel(tier.getTierLevel())
                .status(member.getStatus())
                .newlyEnrolled(newlyEnrolled)
                .build();
    }
}
