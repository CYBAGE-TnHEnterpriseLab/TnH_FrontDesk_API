package com.pms.property.domain.revenue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revenue")
public class RevenueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, unique = true)
    private Long propertyId;

    @Column(name = "channel_manager_enabled", nullable = false)
    private Boolean channelManagerEnabled;

    @Column(name = "commission_percentage", nullable = false)
    private Double commissionPercentage;

    public Long getId() {
        return id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Boolean getChannelManagerEnabled() {
        return channelManagerEnabled;
    }

    public void setChannelManagerEnabled(Boolean channelManagerEnabled) {
        this.channelManagerEnabled = channelManagerEnabled;
    }

    public Double getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(Double commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }
}

