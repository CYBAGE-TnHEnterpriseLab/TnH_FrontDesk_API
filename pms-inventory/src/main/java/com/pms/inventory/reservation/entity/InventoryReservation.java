package com.pms.inventory.reservation.entity;

import com.pms.common.entity.BaseEntity;
import com.pms.inventory.reservation.enums.InventoryReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "confirmation_number", nullable = false, unique = true)
    private String confirmationNumber;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "booked_room_type_id", nullable = false)
    private String bookedRoomTypeId;

    @Column(name = "assigned_room_type_id", nullable = false)
    private String assignedRoomTypeId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryReservationStatus status;
}

