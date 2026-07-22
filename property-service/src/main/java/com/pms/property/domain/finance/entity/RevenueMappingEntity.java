package com.pms.property.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "revenue_mapping")
public class RevenueMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "charge_type", nullable = false)
    private String chargeType;

    @Column(name = "map_gl_account")
    private String mapGlAccount;

    @Column(nullable = false)
    private String status;

    @Column
    private String description;

}



