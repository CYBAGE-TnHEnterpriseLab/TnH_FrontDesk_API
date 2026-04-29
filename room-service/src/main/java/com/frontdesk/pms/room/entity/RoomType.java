package com.frontdesk.pms.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Room Type Name
    @Column(name = "name", nullable = false)
    private String name;

    // Link to Property Service
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    // Master flag
    @Column(name = "is_master", nullable = false)
    private Boolean isMaster;

    // If not master → mapping required
    @Column(name = "master_room_type_id")
    private Long masterRoomTypeId;

    // Audit
    private LocalDateTime createdAt;
}
