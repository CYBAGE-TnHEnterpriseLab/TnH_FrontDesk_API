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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "housekeeping_room_day_status_history",
        indexes = {
                @Index(name = "idx_hk_history_room_date", columnList = "property_id,room_number,business_date,changed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HousekeepingRoomDayStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, length = 36)
    private String propertyId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "room_number", nullable = false, length = 32)
    private String roomNumber;

    @Column(name = "changed_field", nullable = false, length = 64)
    private String changedField;

    @Column(name = "old_value", length = 160)
    private String oldValue;

    @Column(name = "new_value", length = 160)
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", nullable = false, length = 64)
    private StatusChangeSource sourceModule;

    @Column(name = "reason", length = 500)
    private String reason;
}



