package com.pms.loyalty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import com.pms.common.config.BaseOpenApiConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "openapi")
public class OpenApiConfig extends BaseOpenApiConfig {

    private String title;
    private String description;
    private String version;

    @Bean
    public OpenAPI loyaltyOpenApi() {
        return buildOpenApi(new Info()
                .title(title != null ? title : "PMS Loyalty API")
                .description(description != null ? description : "PMS loyalty microservice")
                .version(version != null ? version : "1.0.0"));
    }
}
