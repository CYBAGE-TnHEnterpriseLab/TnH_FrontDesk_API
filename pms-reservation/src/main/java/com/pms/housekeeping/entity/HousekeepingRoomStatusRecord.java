package com.pms.housekeeping.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "housekeeping_room_status", indexes = {
        @Index(name = "idx_hk_property_date_confirmation", columnList = "propertyId,businessDate,confirmationNumber", unique = true),
        @Index(name = "idx_hk_property_date_status", columnList = "propertyId,businessDate,roomStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HousekeepingRoomStatusRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String propertyId;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false, length = 80)
    private String confirmationNumber;

    @Column(length = 20)
    private String roomNo;

    @Column(nullable = false, length = 20)
    private String roomStatus;
}
