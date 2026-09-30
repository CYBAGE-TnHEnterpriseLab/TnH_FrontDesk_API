package com.pms.guestlisting.config;

import com.pms.reservation.config.AvailabilityPerformanceProperties;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.config.InventoryServiceProperties;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.RateManagementAuthInterceptor;
import com.pms.common.config.BaseOpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
    ReservationServiceProperties.class,
    PropertyWizardServiceProperties.class,
    InventoryServiceProperties.class,
    RateManagementServiceProperties.class,
    AvailabilityPerformanceProperties.class
})
public class AppConfig extends BaseOpenApiConfig {

    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 100;

    @Bean
    @Primary
    public RestTemplate restTemplate(ReservationServiceProperties properties) {
        return new RestTemplate(httpRequestFactory(
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs()));
    }

    @Bean
    @Qualifier("rateManagementRestTemplate")
    public RestTemplate rateManagementRestTemplate(RateManagementServiceProperties properties) {
        RestTemplate template = new RestTemplate(httpRequestFactory(
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs()));
        template.getInterceptors().add(new RateManagementAuthInterceptor(properties.getServiceAuthToken()));
        return template;
    }

    private HttpComponentsClientHttpRequestFactory httpRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds((long) connectTimeoutMs + readTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .build();
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(MAX_TOTAL_CONNECTIONS)
                .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
                .build();
        return new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .setConnectionManager(connectionManager)
                        .build());
    }

    @Bean
    public OpenAPI openAPI() {
        return buildOpenApi(new Info()
                .title("Front Desk Arrival API")
                .version("v1")
                .description("APIs for Hotel PMS Arrival Screen")
                .contact(new Contact().name("Front Desk Team").email("frontdesk@hotel.com")));
    }
}

