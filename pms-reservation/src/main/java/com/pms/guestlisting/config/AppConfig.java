package com.pms.guestlisting.config;

import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.config.InventoryServiceProperties;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.RateManagementAuthInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
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
    RateManagementServiceProperties.class
})
public class AppConfig {

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
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();
        return new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Front Desk Arrival API")
                .version("v1")
                .description("APIs for Hotel PMS Arrival Screen")
                .contact(new Contact().name("Front Desk Team").email("frontdesk@hotel.com")));
    }
}

