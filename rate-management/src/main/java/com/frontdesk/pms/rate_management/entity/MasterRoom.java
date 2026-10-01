package com.frontdesk.pms.rate_management.entity;

import com.pms.common.entity.BaseEntity;
import com.frontdesk.pms.rate_management.enums.MasterRoomMealOption;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(name = "uk_master_room_property_name", columnNames = {"property_id", "name"})
)
public class MasterRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id")
    private String propertyId;

    @Column(nullable = false)
    private String name; // e.g., Standard, Deluxe, Suite

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_option")
    private MasterRoomMealOption mealOption;

    @Column(name = "inclusion")
    private String inclusion;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

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

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public List<MasterRoomPricing> getPricingList() {
        return pricingList;
    }

    public void setPricingList(List<MasterRoomPricing> pricingList) {
        this.pricingList = pricingList;
    }

    public MasterRoomMealOption getMealOption() {
        return mealOption;
    }

    public void setMealOption(MasterRoomMealOption mealOption) {
        this.mealOption = mealOption;
    }

    public String getInclusion() {
        return inclusion;
    }

    public void setInclusion(String inclusion) {
        this.inclusion = inclusion;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<MasterRoomRoomTypeMapping> getRoomTypeMappings() {
        return roomTypeMappings;
    }

    public void setRoomTypeMappings(List<MasterRoomRoomTypeMapping> roomTypeMappings) {
        this.roomTypeMappings = roomTypeMappings;
    }
}