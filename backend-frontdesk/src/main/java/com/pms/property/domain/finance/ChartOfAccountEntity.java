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
    private String propertyId;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "ledger_type", nullable = false)
    private String ledgerType;

    @Column(nullable = false)
    private Boolean active;

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getLedgerType() {
        return ledgerType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setLedgerType(String ledgerType) {
        this.ledgerType = ledgerType;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}


