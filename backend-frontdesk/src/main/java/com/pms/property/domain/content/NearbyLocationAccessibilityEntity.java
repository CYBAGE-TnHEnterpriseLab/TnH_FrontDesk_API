package com.pms.property.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nearby_location_accessibility")
public class NearbyLocationAccessibilityEntity {

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

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setDistanceValue(Double distanceValue) {
        this.distanceValue = distanceValue;
    }

    public void setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public void setTravelTimeValue(Integer travelTimeValue) {
        this.travelTimeValue = travelTimeValue;
    }

    public void setTravelTimeUnit(String travelTimeUnit) {
        this.travelTimeUnit = travelTimeUnit;
    }
}


