package com.pms.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PMS Auth API")
                .description("Authentication APIs for PMS")
                .version("v1"));
    }
}

