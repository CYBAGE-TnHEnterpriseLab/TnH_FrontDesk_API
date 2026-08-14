package com.frontdesk.pms.rate_management.dto;

public class PolicyMappingDTO {
    
    class PolicyMapping {
    private String policyId;
    private String ratePlanId;
    private String propertyId;
    private String serviceType;

    // Constructors
    public PolicyMapping() {
    }

    public PolicyMapping(String policyId, String ratePlanId, String propertyId, String serviceType) {
        this.policyId = policyId;
        this.ratePlanId = ratePlanId;
        this.propertyId = propertyId;
        this.serviceType = serviceType; 
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

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}

}
