package com.folio.billing.config;

import com.pms.common.config.BaseOpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

    @Bean
    public OpenAPI folioBillingOpenApi() {
        return buildOpenApi(new Info()
                .title("Folio & Billing API")
                .version("v1")
                .description("APIs for folio billing, charges, payments, and documents."));
    }
}
