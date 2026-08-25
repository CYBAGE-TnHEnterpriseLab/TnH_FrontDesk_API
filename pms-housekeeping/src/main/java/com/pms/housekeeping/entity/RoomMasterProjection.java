package com.pms.housekeeping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "room_master_projection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_master_property_room", columnNames = {"property_id", "room_number"})
        },
        indexes = {
                @Index(name = "idx_room_master_property_type_active", columnList = "property_id,room_type_id,active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMasterProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(name = "room_number", nullable = false, length = 32)
    private String roomNumber;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "room_type_name", nullable = false, length = 120)
    private String roomTypeName;

    @Column(name = "floor", length = 120)
    private String floor;

    @Column(name = "zone", length = 120)
    private String zone;

    @Column(name = "room_class", length = 120)
    private String roomClass;

    @Column(name = "features_csv", length = 4000)
    private String featuresCsv;

    @Column(name = "vip_capable", nullable = false)
    private boolean vipCapable;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


