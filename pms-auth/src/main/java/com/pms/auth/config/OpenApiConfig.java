package com.pms.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import com.pms.common.config.BaseOpenApiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig extends BaseOpenApiConfig {

    private final OpenApiProperties openApiProperties;

    @Bean
    public OpenAPI authOpenApi() {
        return buildOpenApi(new Info()
                .title(openApiProperties.getTitle())
                .description(openApiProperties.getDescription())
                .version(openApiProperties.getVersion()));
    }
}

