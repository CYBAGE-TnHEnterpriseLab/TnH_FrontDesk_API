package com.pms.property.domain.room.entity;

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

}



