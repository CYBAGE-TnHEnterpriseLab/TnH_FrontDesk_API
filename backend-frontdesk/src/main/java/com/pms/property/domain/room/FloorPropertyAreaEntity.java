package com.pms.property.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "floor_property_area")
public class FloorPropertyAreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "floor_name", nullable = false)
    private String floorName;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Column(nullable = false)
    private Integer quantity;

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}


