package com.folio.billing.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "folios", uniqueConstraints = @UniqueConstraint(columnNames = {"confirmation_number", "folio_code"}))
public class Folio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "confirmation_number", nullable = false)
    private String confirmationNumber;
    @Column(name = "folio_code", nullable = false)
    private String folioCode;
    @Column(nullable = false)
    private String guestName;
    @Column(nullable = false)
    private String roomNo;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCharges;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayment;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant lastUpdatedAt;
    //    @Lob private String transactionsJson;
    @Column(name = "transactions_json", columnDefinition = "TEXT")
    private String transactionsJson;

    protected Folio() {
    }

    public Folio(String confirmationNumber, String folioCode, String guestName, String roomNo,
                 BigDecimal totalCharges, BigDecimal totalPayment, BigDecimal outstandingBalance,
                 Instant createdAt, Instant lastUpdatedAt) {
        this.confirmationNumber = confirmationNumber;
        this.folioCode = folioCode;
        this.guestName = guestName;
        this.roomNo = roomNo;
        this.totalCharges = totalCharges;
        this.totalPayment = totalPayment;
        this.outstandingBalance = outstandingBalance;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getFolioCode() {
        return folioCode;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public BigDecimal getTotalCharges() {
        return totalCharges;
    }

    public BigDecimal getTotalPayment() {
        return totalPayment;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getTransactionsJson() {
        return transactionsJson;
    }

    public void setTransactionsJson(String value) {
        this.transactionsJson = value;
    }

    public void update(String guestName, String roomNo, BigDecimal totalCharges, BigDecimal totalPayment,
                       BigDecimal outstandingBalance, Instant lastUpdatedAt) {
        this.guestName = guestName;
        this.roomNo = roomNo;
        this.totalCharges = totalCharges;
        this.totalPayment = totalPayment;
        this.outstandingBalance = outstandingBalance;
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
