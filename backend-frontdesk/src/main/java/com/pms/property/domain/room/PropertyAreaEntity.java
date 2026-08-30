package com.pms.property.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "property_area")
public class PropertyAreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "maximum_capacity", nullable = false)
    private Integer maximumCapacity;

    @Column(length = 1000)
    private String description;

    @Column(name = "amenities_csv", length = 2000)
    private String amenitiesCsv;

    @Column(name = "images_csv", length = 4000)
    private String imagesCsv;

    public String getAreaName() {
        return areaName;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setMaximumCapacity(Integer maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAmenitiesCsv(String amenitiesCsv) {
        this.amenitiesCsv = amenitiesCsv;
    }

    public void setImagesCsv(String imagesCsv) {
        this.imagesCsv = imagesCsv;
    }
}


