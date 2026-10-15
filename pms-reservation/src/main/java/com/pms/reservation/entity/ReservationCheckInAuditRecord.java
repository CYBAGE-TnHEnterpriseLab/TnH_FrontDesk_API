package com.pms.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.pms.common.entity.BaseEntity;

@Entity
@Table(name = "reservation_checkin_audit", indexes = {
        @Index(name = "idx_checkin_audit_booking", columnList = "bookingId"),
        @Index(name = "idx_checkin_audit_confirmation", columnList = "confirmationNumber"),
        @Index(name = "idx_checkin_audit_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationCheckInAuditRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 500)
    private String eventMessage;

    @Column(length = 4000)
    private String changedFields;

    @Column(nullable = false, length = 160)
    private String actor;
}
