package com.frontdesk.pms.rate_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RateManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(RateManagementApplication.class, args);
	}

}
