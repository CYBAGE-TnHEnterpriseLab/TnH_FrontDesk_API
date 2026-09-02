package com.pms.property.domain.property.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "property")
public class PropertyEntity extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "property_code", nullable = false)
    private String propertyCode;

    @Column(name = "property_type", nullable = false)
    private String propertyType;

    @Column(name = "total_no_of_rooms", nullable = false)
    private Integer totalNoOfRooms;

    @Column(name = "total_no_of_floors", nullable = false)
    private Integer totalNoOfFloors;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String website;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "night_audit_time", nullable = false)
    private String nightAuditTime;

    @Column(name = "check_in_time", nullable = false)
    private String checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private String checkOutTime;

    @Column(name = "status", nullable = false)
    private String status;

    @PrePersist
    void assignIdIfMissing() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}



