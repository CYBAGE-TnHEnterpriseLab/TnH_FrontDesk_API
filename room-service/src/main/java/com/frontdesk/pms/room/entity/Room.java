package com.frontdesk.pms.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.frontdesk.pms.room.enums.RoomStatus;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Room Number (auto-generated)
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    private LocalDateTime createdAt;
}
