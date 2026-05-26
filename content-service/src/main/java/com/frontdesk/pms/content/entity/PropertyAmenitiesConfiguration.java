package com.frontdesk.pms.content.entity;

import com.frontdesk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "property_amenities_configuration",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_amenities_property_id", columnNames = "property_id")
)
@Getter
@Setter
public class PropertyAmenitiesConfiguration extends BaseEntity {

    @Column(length = 3)
    private String airportCode;

    @Column(length = 50)
    private String distanceJourneyTime;

    @Column(length = 1000)
    private String directions;

    @Column(nullable = false)
    private boolean groundTransportEnabled;

    @Column(nullable = false)
    private boolean shuttleServiceEnabled;

    @Column(nullable = false)
    private boolean swimmingPoolEnabled;
}
