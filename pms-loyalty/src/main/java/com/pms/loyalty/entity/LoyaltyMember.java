package com.pms.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "loyalty_member")
public class LoyaltyMember {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "loyalty_program_id", nullable = false)
    private UUID loyaltyProgramId;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "loyalty_number", nullable = false, length = 30)
    private String loyaltyNumber;

    @Column(name = "current_tier_id", nullable = false)
    private UUID currentTierId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "enrollment_source", nullable = false, length = 30)
    private String enrollmentSource;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
