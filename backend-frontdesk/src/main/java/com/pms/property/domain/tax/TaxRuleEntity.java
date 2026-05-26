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
    private Long propertyId;

    @Column(name = "tax_name", nullable = false)
    private String taxName;

    @Column(name = "tax_type", nullable = false)
    private String taxType;

    @Column(name = "calculation_type", nullable = false)
    private String calculationType;

    @Column(name = "tax_value", nullable = false)
    private Double value;

    @Column(name = "applies_per_night", nullable = false)
    private Boolean appliesPerNight;

    @Column(nullable = false)
    private Integer priority;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public void setCalculationType(String calculationType) {
        this.calculationType = calculationType;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setAppliesPerNight(Boolean appliesPerNight) {
        this.appliesPerNight = appliesPerNight;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}


