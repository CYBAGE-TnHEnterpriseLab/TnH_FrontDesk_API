package com.pms.property.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_amenity")
public class AmenityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "amenity_code", nullable = false)
    private String amenityCode;

    @Column(nullable = false)
    private Boolean enabled;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setAmenityCode(String amenityCode) {
        this.amenityCode = amenityCode;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

