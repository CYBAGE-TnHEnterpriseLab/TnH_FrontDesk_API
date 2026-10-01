package com.pms.property.domain.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.pms.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nearby_location_accessibility")
public class NearbyLocationAccessibilityEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(nullable = false, length = 40)
    private String section;

    @Column(name = "location_type", nullable = false, length = 120)
    private String locationType;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    @Column(name = "distance_value", nullable = false)
    private Double distanceValue;

    @Column(name = "distance_unit", nullable = false, length = 20)
    private String distanceUnit;

    @Column(name = "travel_time_value", nullable = false)
    private Integer travelTimeValue;

    @Column(name = "travel_time_unit", nullable = false, length = 20)
    private String travelTimeUnit;

}



