package com.pms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PmsAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmsAuthApplication.class, args);
    }
}

