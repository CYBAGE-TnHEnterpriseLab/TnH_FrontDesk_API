package com.pms.property.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_type")
public class RoomTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_master", nullable = false)
    private Boolean master;

    @Column(name = "master_room_name")
    private String masterRoomName;

    @Column(nullable = false)
    private Integer occupancy;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMaster(Boolean master) {
        this.master = master;
    }

    public void setMasterRoomName(String masterRoomName) {
        this.masterRoomName = masterRoomName;
    }

    public void setOccupancy(Integer occupancy) {
        this.occupancy = occupancy;
    }
}

