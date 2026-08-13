package com.folio.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "folio_tax_snapshots")
public class FolioTaxSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String confirmationNo;
    @Column(nullable = false, unique = true) private String transactionReferenceNumber;
    @Lob @Column(nullable = false) private String taxDetailsJson;

    protected FolioTaxSnapshot() { }
    public FolioTaxSnapshot(String confirmationNo, String transactionReferenceNumber, String taxDetailsJson) {
        this.confirmationNo = confirmationNo;
        this.transactionReferenceNumber = transactionReferenceNumber;
        this.taxDetailsJson = taxDetailsJson;
    }
}
