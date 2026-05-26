package com.pms.property.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_method")
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "method_code", nullable = false)
    private String methodCode;

    @Column(name = "ledger_code", nullable = false)
    private String ledgerCode;

    @Column(name = "online_enabled", nullable = false)
    private Boolean onlineEnabled;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }

    public void setLedgerCode(String ledgerCode) {
        this.ledgerCode = ledgerCode;
    }

    public void setOnlineEnabled(Boolean onlineEnabled) {
        this.onlineEnabled = onlineEnabled;
    }
}

