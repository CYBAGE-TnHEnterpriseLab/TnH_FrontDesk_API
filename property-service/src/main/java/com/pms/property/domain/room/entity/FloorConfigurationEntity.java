package com.pms.property.domain.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.pms.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "floor_configuration")
public class FloorConfigurationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "floor_name", nullable = false)
    private String floorName;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName;

    @Column(name = "room_count")
    private Integer roomCount;

    @Column(name = "start_number")
    private Integer startNumber;

}



