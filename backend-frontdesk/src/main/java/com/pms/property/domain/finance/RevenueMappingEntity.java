package com.pms.property.domain.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revenue_mapping")
public class RevenueMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "pms_item", nullable = false)
    private String pmsItem;

    @Column(name = "ledger_code", nullable = false)
    private String ledgerCode;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setPmsItem(String pmsItem) {
        this.pmsItem = pmsItem;
    }

    public void setLedgerCode(String ledgerCode) {
        this.ledgerCode = ledgerCode;
    }
}

