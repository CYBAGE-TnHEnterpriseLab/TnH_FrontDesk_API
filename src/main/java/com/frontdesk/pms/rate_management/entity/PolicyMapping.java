package com.frontdesk.pms.rate_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_mapping")
class PolicyMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private String policyId;
    
    @Column(name = "rate_plan_id")
    private String ratePlanId;
   
    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;
    // Constructors
    public PolicyMapping() {
    }

    public PolicyMapping(String policyId, String ratePlanId) {
        this.policyId = policyId;
        this.ratePlanId = ratePlanId;
    }

    // Getters and Setters
    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(String ratePlanId) {
        this.ratePlanId = ratePlanId;
    }
}
