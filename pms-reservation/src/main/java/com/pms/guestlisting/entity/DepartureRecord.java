package com.pms.guestlisting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.pms.common.entity.BaseEntity;

@Entity
@Table(name = "departure_records", indexes = {
        @Index(name = "idx_departure_property_business_date", columnList = "propertyId,businessDate"),
        @Index(name = "idx_departure_property_business_checkout", columnList = "propertyId,businessDate,checkOutDate"),
        @Index(name = "idx_departure_business_date", columnList = "businessDate"),
        @Index(name = "idx_departure_confirmation", columnList = "confirmationNumber"),
        @Index(name = "idx_departure_guest_name", columnList = "firstName,lastName")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_departure_property_business_confirmation", columnNames = {"propertyId", "businessDate", "confirmationNumber"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DepartureRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(length = 10)
    private String status;

    @Column(length = 20)
    private String salutation;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(length = 15)
    private String roomNo;

    @Column(length = 40)
    private String reservationType;

    @Column(length = 80)
    private String city;

    @Column(length = 40)
    private String rateCode;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private Integer roomNights;

    @Column(length = 40)
    private String roomStatus;

    @Column(length = 40)
    private String corporateCode;

    @Column(length = 40)
    private String roomType;

    @Column(nullable = false, length = 60)
    private String confirmationNumber;

    @Column(length = 100)
    private String company;

    @Column(length = 1)
    private String sharingStatus;

    private Integer floor;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(length = 60)
    private String loyaltyMembershipStatus;

    @Column(nullable = false)
    private LocalDateTime sourceLastSyncedAt;
}

