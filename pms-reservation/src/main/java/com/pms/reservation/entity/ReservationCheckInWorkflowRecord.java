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

@Entity
@Table(name = "reservation_checkin_workflow", indexes = {
        @Index(name = "idx_checkin_workflow_booking", columnList = "bookingId", unique = true),
        @Index(name = "idx_checkin_workflow_confirmation", columnList = "confirmationNumber")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCheckInWorkflowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false, length = 40)
    private String currentStep;

    private LocalDateTime guestDetailsCompletedAt;

    private LocalDateTime roomStayCompletedAt;

    private LocalDateTime signatureCompletedAt;

    private LocalDateTime paymentValidatedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
