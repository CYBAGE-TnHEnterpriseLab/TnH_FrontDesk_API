package com.pms.reservation.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reservation_checkin_id_proofs", indexes = {
        @Index(name = "idx_checkin_id_proof_booking", columnList = "bookingId", unique = true),
        @Index(name = "idx_checkin_id_proof_confirmation", columnList = "confirmationNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationCheckInIdProofRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(length = 20)
    private String checkInChannel;

    @Column(nullable = false, length = 40)
    private String idProofType;

    @Column(nullable = false, length = 120)
    private String idProofNumber;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadBase64;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}