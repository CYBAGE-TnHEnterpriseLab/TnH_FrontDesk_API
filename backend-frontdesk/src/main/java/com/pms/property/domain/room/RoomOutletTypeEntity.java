package com.pms.property.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_outlet_type")
public class RoomOutletTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

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

    public String getRoomName() {
        return roomName;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setAvailableForSell(Boolean availableForSell) {
        this.availableForSell = availableForSell;
    }

    public void setMaximumGuestOccupancy(Integer maximumGuestOccupancy) {
        this.maximumGuestOccupancy = maximumGuestOccupancy;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAmenitiesCsv(String amenitiesCsv) {
        this.amenitiesCsv = amenitiesCsv;
    }

    public void setImagesCsv(String imagesCsv) {
        this.imagesCsv = imagesCsv;
    }
}


