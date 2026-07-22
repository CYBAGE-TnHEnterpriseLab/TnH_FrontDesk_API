package com.pms.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservation_bookings", indexes = {
        @Index(name = "idx_reservation_property_arrival", columnList = "propertyId,arrivalDate"),
        @Index(name = "idx_reservation_arrival_departure", columnList = "arrivalDate,departureDate"),
    @Index(name = "idx_reservation_guest_name", columnList = "guestName"),
    @Index(name = "idx_reservation_confirmation", columnList = "confirmationNumber")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationBookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String confirmationNumber;

    @Column(nullable = false, length = 30)
    private String reservationStatus;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false, length = 20)
    private String salutation;

    @Column(nullable = false)
    private Boolean vipTag;

    @Column(nullable = false, length = 160)
    private String guestName;

    @Column(nullable = false, length = 4000)
    private String guestNamesEncoded;

    @Column(nullable = false, length = 160)
    private String personalEmail;

    @Column(nullable = false, length = 160)
    private String officialEmail;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String mobileNumber;

    @Column(length = 40)
    private String loyaltyNumber;

    @Column(length = 120)
    private String company;

    @Column(length = 120)
    private String guestGroup;

    @Column(length = 120)
    private String source;

    @Column(length = 120)
    private String agent;

    @Column(nullable = false)
    private LocalDate arrivalDate;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private Integer adultCount;

    @Column(nullable = false)
    private Integer childCount;

    @Column(nullable = false, length = 20)
    private String reservationType;

    @Column(nullable = false, length = 40)
    private String roomType;

    @Column(length = 20)
    private String assignedRoomNo;

    private Integer floor;

    @Column(nullable = false, length = 40)
    private String rateCode;

    @Column(nullable = false)
    private Integer numberOfRooms;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRate;

    @Column(nullable = false, length = 40)
    private String payment;

    @Column(nullable = false, length = 40)
    private String paymentType;

    @Column(nullable = false)
    private LocalTime eta;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    @Column(nullable = false)
    private Boolean dnm;

    @Column(nullable = false)
    private Boolean noPost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal guestBalance;

    @Column(length = 500)
    private String specialRequests;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(length = 500)
    private String alertsMessages;

    private LocalDateTime inventoryDeductedAt;

    private LocalDateTime inventorySyncedAt;

    private LocalDateTime checkInCompletedAt;

    @Column(length = 160)
    private String checkInCompletedBy;

    private LocalDate checkInBusinessDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
