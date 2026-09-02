package com.pms.reservation.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.pms.common.entity.BaseEntity;

@Entity
@Table(name = "reservation_checkin_signatures", indexes = {
        @Index(name = "idx_checkin_signature_booking", columnList = "bookingId", unique = true),
        @Index(name = "idx_checkin_signature_confirmation", columnList = "confirmationNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationCheckInSignatureRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadBase64;

    @Column(nullable = false)
    private LocalDateTime signedAt;
}
