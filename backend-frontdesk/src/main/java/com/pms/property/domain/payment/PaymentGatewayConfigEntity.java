package com.pms.property.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_gateway_config")
public class PaymentGatewayConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, unique = true)
    private Long propertyId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "api_key_encrypted", nullable = false)
    private String apiKeyEncrypted;

    @Column(name = "secret_encrypted", nullable = false)
    private String secretEncrypted;

    @Column(nullable = false)
    private String mode;

    @Column(name = "auto_capture", nullable = false)
    private Boolean autoCapture;

    @Column(name = "three_ds_enabled", nullable = false)
    private Boolean threeDsEnabled;

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public void setSecretEncrypted(String secretEncrypted) {
        this.secretEncrypted = secretEncrypted;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setAutoCapture(Boolean autoCapture) {
        this.autoCapture = autoCapture;
    }

    public void setThreeDsEnabled(Boolean threeDsEnabled) {
        this.threeDsEnabled = threeDsEnabled;
    }
}

