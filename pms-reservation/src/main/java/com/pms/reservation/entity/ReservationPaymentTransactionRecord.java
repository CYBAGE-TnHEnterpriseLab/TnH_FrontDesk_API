package com.pms.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.pms.common.entity.BaseEntity;

@Entity
@Table(name = "reservation_payment_transactions", indexes = {
        @Index(name = "idx_payment_txn_booking", columnList = "bookingId"),
        @Index(name = "idx_payment_txn_confirmation", columnList = "confirmationNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationPaymentTransactionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false, length = 40)
    private String paymentMode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String transactionStatus;

    @Column(nullable = false, length = 120)
    private String transactionReference;

    @Column(nullable = false, length = 80)
    private String processorName;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
