package com.pms.property.domain.room.entity;

import com.pms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "room_outlet_type")
public class RoomOutletTypeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "room_code", length = 80)
    private String roomCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "available_for_sell", nullable = false)
    private Boolean availableForSell;

    @Column(name = "maximum_guest_occupancy", nullable = false)
    private Integer maximumGuestOccupancy;

    @Column(length = 1000)
    private String description;

    @Column(name = "amenities_csv", length = 2000)
    private String amenitiesCsv;

    @Column(name = "images_csv", length = 4000)
    private String imagesCsv;

}



