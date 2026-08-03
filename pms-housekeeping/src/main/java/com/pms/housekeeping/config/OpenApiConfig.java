package com.pms.housekeeping.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI housekeepingOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("PMS Housekeeping API")
                        .version("v1.0")
                        .description("REST APIs for PMS Housekeeping Microservice")
                        .contact(new Contact()
                                .name("PMS Team")
                                .email("support@pms.com"))
                        .license(new License()
                                .name("Internal Use")));
    }
}