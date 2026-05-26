package com.pms.property.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_room")
public class InventoryRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "floor_name", nullable = false)
    private String floorName;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
}

