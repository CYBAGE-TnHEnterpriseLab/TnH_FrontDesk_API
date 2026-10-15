package com.pms.loyalty.service;

import com.pms.loyalty.dto.response.LoyaltyMembershipResponse;
import com.pms.loyalty.entity.LoyaltyMember;
import com.pms.loyalty.entity.LoyaltyTier;
import com.pms.loyalty.exception.LoyaltyConfigurationException;
import com.pms.loyalty.exception.LoyaltyNumberNotFoundException;
import com.pms.loyalty.repository.LoyaltyMemberRepository;
import com.pms.loyalty.repository.LoyaltyTierRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LoyaltyEnrollmentService {

    private final LoyaltyTierRepository loyaltyTierRepository;
    private final LoyaltyMemberRepository loyaltyMemberRepository;
    private final LoyaltyNumberGenerator loyaltyNumberGenerator;
    private final Random random = new Random();

    @Transactional
    public LoyaltyMembershipResponse enroll(UUID guestId, UUID loyaltyProgramId) {
        if (guestId == null || loyaltyProgramId == null) {
            throw new LoyaltyConfigurationException("guestId and loyaltyProgramId are required to enroll a member");
        }

        List<LoyaltyTier> activeTiers = loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(loyaltyProgramId);
        if (activeTiers.isEmpty()) {
            throw new LoyaltyConfigurationException(
                    "No active loyalty tiers configured for loyaltyProgramId=" + loyaltyProgramId);
        }

        LoyaltyTier assignedTier = activeTiers.get(random.nextInt(activeTiers.size()));

        String loyaltyNumber = loyaltyNumberGenerator.generateLoyaltyNumber();
        LocalDateTime now = LocalDateTime.now();

        LoyaltyMember member = LoyaltyMember.builder()
                .id(UUID.randomUUID())
                .loyaltyProgramId(loyaltyProgramId)
                .guestId(guestId)
                .loyaltyNumber(loyaltyNumber)
                .currentTierId(assignedTier.getId())
                .status("ACTIVE")
                .enrollmentSource("BOOKING")
                .enrolledAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        LoyaltyMember saved = loyaltyMemberRepository.save(member);

        return toResponse(saved, assignedTier, true);
    }

    /**
     * Returns the membership a guest already holds, creating one only when the guest is not enrolled yet.
     * An existing membership is never re-enrolled: the originally assigned tier and loyalty number are preserved.
     */
    @Transactional
    public LoyaltyMembershipResponse ensureMembership(UUID guestId, UUID loyaltyProgramId) {
        Optional<LoyaltyMember> existingMember = guestId == null || loyaltyProgramId == null
                ? Optional.empty()
                : loyaltyMemberRepository.findByGuestIdAndLoyaltyProgramId(guestId, loyaltyProgramId);

        if (existingMember.isPresent()) {
            LoyaltyMember member = existingMember.get();
            return toResponse(member, resolveCurrentTier(member), false);
        }

        return enroll(guestId, loyaltyProgramId);
    }

    @Transactional(readOnly = true)
    public LoyaltyMembershipResponse verifyLoyaltyNumber(String loyaltyNumber) {
        if (!StringUtils.hasText(loyaltyNumber)) {
            throw new IllegalArgumentException("loyaltyNumber is required");
        }

        LoyaltyMember member = loyaltyMemberRepository.findByLoyaltyNumber(loyaltyNumber.trim())
                .orElseThrow(() -> new LoyaltyNumberNotFoundException(loyaltyNumber.trim()));

        return toResponse(member, resolveCurrentTier(member), false);
    }

    private LoyaltyTier resolveCurrentTier(LoyaltyMember member) {
        return loyaltyTierRepository.findById(member.getCurrentTierId())
                .orElseThrow(() -> new LoyaltyConfigurationException(
                        "Loyalty tier not found for currentTierId=" + member.getCurrentTierId()));
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
