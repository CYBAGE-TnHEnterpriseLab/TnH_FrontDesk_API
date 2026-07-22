package com.pms.inventory.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI inventoryOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("PMS Inventory API")
						.version("v1")
						.description("Room-type-level inventory management API")
						.contact(new Contact().name("PMS Platform Team")));
	}
}

