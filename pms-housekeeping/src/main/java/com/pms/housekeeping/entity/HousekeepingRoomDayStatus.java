package com.pms.housekeeping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "housekeeping_room_day_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hk_room_day", columnNames = {"property_id", "business_date", "room_number"})
        },
        indexes = {
                @Index(name = "idx_hk_room_day_property_date", columnList = "property_id,business_date"),
                @Index(name = "idx_hk_room_day_filters", columnList = "property_id,business_date,room_type_id,cleaning_status,front_office_status,reservation_status,is_sellable")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HousekeepingRoomDayStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "room_number", nullable = false, length = 32)
    private String roomNumber;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_status", nullable = false, length = 32)
    private CleaningStatus cleaningStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "front_office_status", nullable = false, length = 32)
    private FrontOfficeStatus frontOfficeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 32)
    private ReservationStatus reservationStatus;

    @Column(name = "assigned_reservation_id")
    private UUID assignedReservationId;

    @Column(name = "attendant_name", length = 160)
    private String attendantName;

    @Column(name = "last_cleaned_at")
    private LocalDateTime lastCleanedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private HousekeepingPriority priority;

    @Column(name = "is_sellable", nullable = false)
    private boolean sellable;

    @Column(name = "guest_display_name", length = 200)
    private String guestDisplayName;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "fo_status_changed_at")
    private LocalDateTime foStatusChangedAt;

    @Column(name = "reservation_status_changed_at")
    private LocalDateTime reservationStatusChangedAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


