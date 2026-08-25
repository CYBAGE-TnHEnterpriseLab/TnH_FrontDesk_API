package com.pms.housekeeping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "housekeeping_room_day_status",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hk_room_day",
                        columnNames = {"property_id", "business_date", "room_number"}
                )
        },
        indexes = {

                @Index(
                        name = "idx_hk_property_date",
                        columnList = "property_id,business_date"
                ),

                @Index(
                        name = "idx_hk_room_type",
                        columnList = "property_id,business_date,room_type_id"
                ),

                @Index(
                        name = "idx_hk_cleaning",
                        columnList = "property_id,business_date,cleaning_status"
                ),

                @Index(
                        name = "idx_hk_frontoffice",
                        columnList = "property_id,business_date,front_office_status"
                ),

                @Index(
                        name = "idx_hk_reservation",
                        columnList = "property_id,business_date,reservation_status"
                ),

                @Index(
                        name = "idx_hk_floor",
                        columnList = "property_id,business_date,floor"
                ),

                @Index(
                        name = "idx_hk_attendant",
                        columnList = "property_id,business_date,attendant_name"
                )
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
    private String propertyId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "room_number", nullable = false, length = 32)
    private String roomNumber;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "room_type_name", nullable = false, length = 100)
    private String roomTypeName;

    @Column(name = "floor", length = 50)
    private String floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_status", nullable = false, length = 30)
    private CleaningStatus cleaningStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "front_office_status", nullable = false, length = 30)
    private FrontOfficeStatus frontOfficeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 30)
    private ReservationStatus reservationStatus;

    @Column(name = "confirmation_id", length = 50)
    private String confirmationId;

    @Column(name = "guest_display_name", length = 200)
    private String guestDisplayName;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "attendant_name", length = 160)
    private String attendantName;

    @Column(name = "last_cleaned_at")
    private LocalDateTime lastCleanedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private HousekeepingPriority priority;

    @Column(name = "is_sellable", nullable = false)
    private boolean sellable;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "features_csv", length = 500)
    private String featuresCsv;
}


