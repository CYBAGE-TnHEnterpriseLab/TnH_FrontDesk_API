package com.frontdesk.pms.rate_management.entity;

import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_type_occupancy", columnNames = {"room_type_id", "occupancy_type"})
    }
)
public class MasterRoomPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // For master pricing, this is set. For child pricing, this can be null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_room_id")
    private MasterRoom masterRoom;

    // For child pricing, this is set. For master pricing, this is null.
    @Column(name = "room_type_id")
    private Long roomTypeId;

    // Indicates if this price is inherited from a master room
    @Column(name = "inherited")
    private Boolean inherited = false;

    // Optionally, reference to the master pricing id for traceability
    @Column(name = "parent_pricing_id")
    private Long parentPricingId;

    @Column(nullable = false)
    private String occupancyType; // 1 Guest, 2 Guest, 3 Guest, 4 Guest, Extra Guest Charges(1P), Extra Guest Charges(2P)

    @Column(nullable = false)
    private Double price;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MasterRoom getMasterRoom() {
        return masterRoom;
    }

    public void setMasterRoom(MasterRoom masterRoom) {
        this.masterRoom = masterRoom;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Boolean getInherited() {
        return inherited;
    }

    public void setInherited(Boolean inherited) {
        this.inherited = inherited;
    }

    public Long getParentPricingId() {
        return parentPricingId;
    }

    public void setParentPricingId(Long parentPricingId) {
        this.parentPricingId = parentPricingId;
    }

    public String getOccupancyType() {
        return occupancyType;
    }

    public void setOccupancyType(String occupancyType) {
        this.occupancyType = occupancyType;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}