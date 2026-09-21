package com.frontdesk.pms.rate_management.config;

import com.pms.common.config.BaseOpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

    @Bean
    public OpenAPI rateManagementOpenAPI() {
        return buildOpenApi(new Info()
                .title("Rate Management API")
                .version("v1")
                .description("APIs for Rate and Room Management")
                .contact(new Contact().name("PMS Team").email("support@pms.com")));
    }
}
