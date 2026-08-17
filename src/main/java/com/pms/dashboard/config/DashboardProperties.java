package com.pms.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "dashboard")
public class DashboardProperties {

    private long timeoutMs = 2500;

    private ServiceProperties housekeeping = new ServiceProperties(
            "http://localhost:8086",
            "/api/v1/housekeeping/dashboard",
            "/api/v1/housekeeping/rooms"
    );

    private ServiceProperties inventory = new ServiceProperties(
            "http://localhost:8085",
            "/api/v1/inventory/daily",
            null
    );

    private ServiceProperties property = new ServiceProperties(
            "http://localhost:8082",
            "/api/rooms/properties/{propertyId}/room-outlet-types",
            null
    );

    private ServiceProperties rate = new ServiceProperties(
            "http://localhost:8087",
            "/api/rate-plans/property/{propertyId}",
            null
    );

    private ServiceProperties reservation = new ServiceProperties(
            "http://localhost:8090",
            "/api/v1/guest-listing/list",
            null
    );

    @Getter
    @Setter
    public static class ServiceProperties {
        private String baseUrl;
        private String primaryPath;
        private String secondaryPath;

        public ServiceProperties() {
        }

        public ServiceProperties(String baseUrl, String primaryPath, String secondaryPath) {
            this.baseUrl = baseUrl;
            this.primaryPath = primaryPath;
            this.secondaryPath = secondaryPath;
        }
    }
}

