package com.pms.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "loyalty_tier")
public class LoyaltyTier {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "loyalty_program_id", nullable = false)
    private UUID loyaltyProgramId;

    @Column(name = "tier_code", nullable = false, length = 30)
    private String tierCode;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName;

    @Column(name = "tier_level", nullable = false)
    private Integer tierLevel;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
