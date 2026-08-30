package com.pms.property.domain.tax;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tax_rule")
public class TaxRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "tax_name", nullable = false)
    private String taxName;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "applicable_on", nullable = false)
    private String applicableOn;

    @Column(name = "incl_excl", nullable = false)
    private String inclExcl;

    @Column(name = "effective_date", nullable = false)
    private String effectiveDate;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer priority;

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public void setApplicableOn(String applicableOn) {
        this.applicableOn = applicableOn;
    }

    public void setInclExcl(String inclExcl) {
        this.inclExcl = inclExcl;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}



