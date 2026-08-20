package com.folio.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private ServiceConfig reservationService = new ServiceConfig();

    public ServiceConfig getReservationService() {
        return reservationService;
    }

    public void setReservationService(ServiceConfig reservationService) {
        this.reservationService = reservationService;
    }

    public static class ServiceConfig {

        private boolean enabled;
        private String baseUrl;
        private String propertyId;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public void setPropertyId(String propertyId) {
            this.propertyId = propertyId;
        }
    }

}
