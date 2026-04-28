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
        name = "property_special_requests_configuration",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_special_requests_property_id", columnNames = "property_id")
)
@Getter
@Setter
public class PropertySpecialRequestsConfiguration extends BaseEntity {

    @Column(nullable = false)
    private boolean extraPillowEnabled;

    @Column(nullable = false)
    private boolean babyCribEnabled;

    @Column(nullable = false)
    private boolean lateCheckOutEnabled;

    @Column(nullable = false)
    private boolean hypoallergenicBeddingEnabled;

    @Column(nullable = false)
    private boolean airportPickupEnabled;

    @Column(nullable = false)
    private boolean wheelchairAccessEnabled;
}
