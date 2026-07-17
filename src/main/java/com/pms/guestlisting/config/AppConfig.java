package com.pms.guestlisting.config;

import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.config.RateManagementServiceProperties;
import com.pms.security.config.JwtSecurityProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
    ReservationServiceProperties.class,
    PropertyWizardServiceProperties.class,
    RateManagementServiceProperties.class,
    JwtSecurityProperties.class
})
public class AppConfig {

    @Value("${app.cors.allowed-origins:http://172.27.57.97:4200,http://localhost:5173}")
    private List<String> corsAllowedOrigins;

    @Bean
    public RestTemplate restTemplate(ReservationServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return new RestTemplate(factory);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Front Desk Arrival API")
                .version("v1")
                .description("APIs for Hotel PMS Arrival Screen")
                .contact(new Contact().name("Front Desk Team").email("frontdesk@hotel.com")));
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(corsAllowedOrigins.toArray(String[]::new))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
}

