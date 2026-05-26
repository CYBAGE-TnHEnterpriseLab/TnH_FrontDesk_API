package com.pms.property.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_special_request")
public class SpecialRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "request_code", nullable = false)
    private String requestCode;

    @Column(nullable = false)
    private Boolean enabled;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

