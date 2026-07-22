package com.pms.property.integration.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class InventorySyncWebClientConfig {

    @Bean
    public WebClient inventorySyncWebClient(
        WebClient.Builder builder,
        @Value("${inventory.sync.base-url:http://localhost:8085}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient housekeepingSyncWebClient(
        WebClient.Builder builder,
        @Value("${housekeeping.sync.base-url:http://localhost:8086}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}


