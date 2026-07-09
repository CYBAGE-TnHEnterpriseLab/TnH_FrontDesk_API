package com.pms.property.domain.content.entity;

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
@Table(name = "property_overview")
public class PropertyOverviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, unique = true)
    private String propertyId;

    @Column(name = "property_hero_image", nullable = false, length = 500)
    private String propertyHeroImage;

    @Column(name = "property_description", nullable = false, length = 2000)
    private String propertyDescription;

}



