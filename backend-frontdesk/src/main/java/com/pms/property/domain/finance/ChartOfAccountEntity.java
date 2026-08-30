package com.pms.property.domain.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chart_of_account")
public class ChartOfAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "ledger_code", nullable = false)
    private String ledgerCode;

    @Column(name = "ledger_name", nullable = false)
    private String ledgerName;

    @Column(nullable = false)
    private String category;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getLedgerCode() {
        return ledgerCode;
    }

    public void setLedgerCode(String ledgerCode) {
        this.ledgerCode = ledgerCode;
    }

    public void setLedgerName(String ledgerName) {
        this.ledgerName = ledgerName;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

