package com.frontdesk.pms.entity;

import com.frontdesk.common.entity.BaseEntity;
import com.frontdesk.common.enums.PropertyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "properties",
        indexes = {
                @Index(name = "idx_property_name", columnList = "name"),
                @Index(name = "idx_property_email", columnList = "email"),
                @Index(name = "idx_property_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property extends BaseEntity {

    @Column(unique = true, nullable = false)
    @Size(max = 120)
    private String name;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String address;

    @NotBlank
    private String contactName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$")
    private String contactNumber;

    @NotBlank
    private String timeZone;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private LocalTime nightAuditTime;

    @Enumerated(EnumType.STRING)
    private PropertyStatus status;
}