package com.pms.inventory.common.config;

import com.pms.common.config.BaseOpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {

	@Bean
	public OpenAPI inventoryOpenApi() {
		return buildOpenApi(new Info()
				.title("PMS Inventory API")
				.version("v1")
				.description("Room-type-level inventory management API")
				.contact(new Contact().name("PMS Platform Team")));
	}
}

