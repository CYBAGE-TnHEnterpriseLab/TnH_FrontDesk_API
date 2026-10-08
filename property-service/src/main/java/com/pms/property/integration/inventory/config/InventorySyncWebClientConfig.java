package com.pms.property.integration.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import reactor.netty.http.client.HttpClient;

@Configuration
public class InventorySyncWebClientConfig {

    @Bean
    public WebClient inventorySyncWebClient(
        WebClient.Builder builder,
        @Value("${inventory.sync.base-url:http://localhost:8085}") String baseUrl,
        @Value("${inventory.sync.timeout-seconds:120}") int timeoutSeconds
    ) {
        Duration timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 1));
        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(timeout)
                ))
                .build();
    }

    @Bean
    public WebClient housekeepingSyncWebClient(
        WebClient.Builder builder,
        @Value("${housekeeping.sync.base-url:http://localhost:8086}") String baseUrl,
        @Value("${inventory.sync.timeout-seconds:120}") int timeoutSeconds
    ) {
        Duration timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 1));
        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(timeout)
                ))
                .build();
    }
}


