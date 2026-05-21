package com.frontdesk.pms.rate_management.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class MasterRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., Standard, Deluxe, Suite

    @OneToMany(mappedBy = "masterRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MasterRoomPricing> pricingList;

    @OneToMany(mappedBy = "masterRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MasterRoomRoomTypeMapping> roomTypeMappings;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MasterRoomPricing> getPricingList() {
        return pricingList;
    }

    public void setPricingList(List<MasterRoomPricing> pricingList) {
        this.pricingList = pricingList;
    }

    public List<MasterRoomRoomTypeMapping> getRoomTypeMappings() {
        return roomTypeMappings;
    }

    public void setRoomTypeMappings(List<MasterRoomRoomTypeMapping> roomTypeMappings) {
        this.roomTypeMappings = roomTypeMappings;
    }
}