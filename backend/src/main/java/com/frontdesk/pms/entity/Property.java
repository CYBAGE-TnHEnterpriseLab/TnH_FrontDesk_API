package com.frontdesk.pms.entity;

import com.frontdesk.pms.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "property")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_name", nullable = false, unique = true, length = 150)
    private String propertyName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 20)
    private String contact;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "night_audit_time", nullable = false)
    private String nightAuditTime;

    @Column(name = "checkin_time", nullable = false)
    private String checkinTime;

    @Column(name = "checkout_time", nullable = false)
    private String checkoutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}