package com.frontdesk.pms.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "floor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    private LocalDateTime createdAt;
}
