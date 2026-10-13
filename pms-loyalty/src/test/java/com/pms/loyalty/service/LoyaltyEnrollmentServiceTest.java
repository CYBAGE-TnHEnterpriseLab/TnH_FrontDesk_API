package com.pms.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.loyalty.entity.LoyaltyMember;
import com.pms.loyalty.entity.LoyaltyTier;
import com.pms.loyalty.exception.LoyaltyConfigurationException;
import com.pms.loyalty.exception.LoyaltyNumberNotFoundException;
import com.pms.loyalty.repository.LoyaltyMemberRepository;
import com.pms.loyalty.repository.LoyaltyTierRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoyaltyEnrollmentServiceTest {

    private static final UUID GUEST_ID = UUID.randomUUID();
    private static final UUID OTHER_GUEST_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID SILVER_ID = UUID.fromString("22345678-1234-1234-1234-123456789012");
    private static final UUID GOLD_ID = UUID.fromString("32345678-1234-1234-1234-123456789012");

    @Mock
    private LoyaltyTierRepository loyaltyTierRepository;

    @Mock
    private LoyaltyMemberRepository loyaltyMemberRepository;

    private LoyaltyEnrollmentService service;

    @Mock
    private LoyaltyNumberGenerator loyaltyNumberGenerator;

    @BeforeEach
    void setUp() {
        service = new LoyaltyEnrollmentService(
                loyaltyTierRepository, loyaltyMemberRepository, loyaltyNumberGenerator);
    }

    @Test
    void enrollAssignsGeneratedNumberAndActiveTierToNewMember() {
        when(loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(PROGRAM_ID))
                .thenReturn(List.of(silverTier(), goldTier()));
        when(loyaltyNumberGenerator.generateLoyaltyNumber()).thenReturn("LOY00000001");
        when(loyaltyMemberRepository.save(any(LoyaltyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.enroll(GUEST_ID, PROGRAM_ID);

        assertThat(response.getLoyaltyNumber()).isEqualTo("LOY00000001");
        assertThat(response.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(response.isNewlyEnrolled()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(List.of("SILVER", "GOLD")).contains(response.getTierCode());
        var captor = org.mockito.ArgumentCaptor.forClass(LoyaltyMember.class);
        verify(loyaltyMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentTierId()).isIn(SILVER_ID, GOLD_ID);
        // A pre-set version makes Spring Data treat the row as existing and merge it, which fails with
        // StaleObjectStateException against a real database. It must stay null so the member is persisted.
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void enrollRejectsProgramWithoutActiveTiers() {
        when(loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(PROGRAM_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.enroll(GUEST_ID, PROGRAM_ID))
                .isInstanceOf(LoyaltyConfigurationException.class)
                .hasMessageContaining("No active loyalty tiers configured");
        verify(loyaltyMemberRepository, never()).save(any());
    }

    @Test
    void ensureMembershipReusesExistingMemberWithoutReassigningTierOrNumber() {
        LoyaltyMember existing = member(GUEST_ID, "LOY00000009", GOLD_ID);
        when(loyaltyMemberRepository.findByGuestIdAndLoyaltyProgramId(GUEST_ID, PROGRAM_ID))
                .thenReturn(Optional.of(existing));
        when(loyaltyTierRepository.findById(GOLD_ID)).thenReturn(Optional.of(goldTier()));

        var response = service.ensureMembership(GUEST_ID, PROGRAM_ID);

        assertThat(response.getLoyaltyNumber()).isEqualTo("LOY00000009");
        assertThat(response.getTierCode()).isEqualTo("GOLD");
        assertThat(response.getTierName()).isEqualTo("Gold");
        assertThat(response.getTierLevel()).isEqualTo(2);
        assertThat(response.isNewlyEnrolled()).isFalse();
        verify(loyaltyMemberRepository, never()).save(any());
        verify(loyaltyNumberGenerator, never()).generateLoyaltyNumber();
    }

    @Test
    void ensureMembershipEnrollsOnlyWhenNoMembershipExists() {
        when(loyaltyMemberRepository.findByGuestIdAndLoyaltyProgramId(GUEST_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());
        when(loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(PROGRAM_ID))
                .thenReturn(List.of(silverTier()));
        when(loyaltyNumberGenerator.generateLoyaltyNumber()).thenReturn("LOY00000002");
        when(loyaltyMemberRepository.save(any(LoyaltyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.ensureMembership(GUEST_ID, PROGRAM_ID);

        assertThat(response.getLoyaltyNumber()).isEqualTo("LOY00000002");
        assertThat(response.isNewlyEnrolled()).isTrue();
        verify(loyaltyMemberRepository).save(any());
    }

    @Test
    void existingMemberOfAnotherGuestIsNotReused() {
        when(loyaltyMemberRepository.findByGuestIdAndLoyaltyProgramId(OTHER_GUEST_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());
        when(loyaltyTierRepository.findByLoyaltyProgramIdAndActiveTrue(PROGRAM_ID))
                .thenReturn(List.of(silverTier()));
        when(loyaltyNumberGenerator.generateLoyaltyNumber()).thenReturn("LOY00000003");
        when(loyaltyMemberRepository.save(any(LoyaltyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.ensureMembership(OTHER_GUEST_ID, PROGRAM_ID);

        assertThat(response.getGuestId()).isEqualTo(OTHER_GUEST_ID);
        assertThat(response.getLoyaltyNumber()).isEqualTo("LOY00000003");
    }

    @Test
    void verifyLoyaltyNumberReturnsMembershipItIsAssignedTo() {
        when(loyaltyMemberRepository.findByLoyaltyNumber("LOY00000042"))
                .thenReturn(Optional.of(member(GUEST_ID, "LOY00000042", SILVER_ID)));
        when(loyaltyTierRepository.findById(SILVER_ID)).thenReturn(Optional.of(silverTier()));

        var response = service.verifyLoyaltyNumber("LOY00000042");

        assertThat(response.getLoyaltyNumber()).isEqualTo("LOY00000042");
        assertThat(response.getTierCode()).isEqualTo("SILVER");
        assertThat(response.getGuestId()).isEqualTo(GUEST_ID);
    }

    @Test
    void verifyLoyaltyNumberRejectsUnknownNumber() {
        when(loyaltyMemberRepository.findByLoyaltyNumber("LOY99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyLoyaltyNumber("LOY99999999"))
                .isInstanceOf(LoyaltyNumberNotFoundException.class)
                .hasMessageContaining("loyalty number invalid and no guest profile found");
    }

    @Test
    void verifyLoyaltyNumberRejectsBlankInput() {
        assertThatThrownBy(() -> service.verifyLoyaltyNumber("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loyaltyNumber is required");
    }

    private LoyaltyMember member(UUID guestId, String loyaltyNumber, UUID tierId) {
        LocalDateTime now = LocalDateTime.now();
        return LoyaltyMember.builder()
                .id(UUID.randomUUID())
                .guestId(guestId)
                .loyaltyProgramId(PROGRAM_ID)
                .loyaltyNumber(loyaltyNumber)
                .currentTierId(tierId)
                .status("ACTIVE")
                .enrollmentSource("BOOKING")
                .enrolledAt(now)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    private LoyaltyTier silverTier() {
        return LoyaltyTier.builder()
                .id(SILVER_ID)
                .loyaltyProgramId(PROGRAM_ID)
                .tierCode("SILVER")
                .tierName("Silver")
                .tierLevel(1)
                .active(Boolean.TRUE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private LoyaltyTier goldTier() {
        return LoyaltyTier.builder()
                .id(GOLD_ID)
                .loyaltyProgramId(PROGRAM_ID)
                .tierCode("GOLD")
                .tierName("Gold")
                .tierLevel(2)
                .active(Boolean.TRUE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
