package com.pms.guestlisting.config;

import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.config.HousekeepingServiceProperties;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.reservation.integration.RateManagementAuthInterceptor;
import com.pms.security.config.JwtSecurityProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
    ReservationServiceProperties.class,
    PropertyWizardServiceProperties.class,
    HousekeepingServiceProperties.class,
    RateManagementServiceProperties.class,
    JwtSecurityProperties.class
})
public class AppConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(ReservationServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return new RestTemplate(factory);
    }

    @Bean
    @Qualifier("rateManagementRestTemplate")
    public RestTemplate rateManagementRestTemplate(RateManagementServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        RestTemplate template = new RestTemplate(factory);
        template.getInterceptors().add(new RateManagementAuthInterceptor(properties.getServiceAuthToken()));
        return template;
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

