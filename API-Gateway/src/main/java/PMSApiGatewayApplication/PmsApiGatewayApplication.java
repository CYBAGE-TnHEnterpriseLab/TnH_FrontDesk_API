package PMSApiGatewayApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PmsApiGatewayApplication {

	public static void main(String[] args) {

		SpringApplication.run(PmsApiGatewayApplication.class, args);

		System.out.println("API Gateway is running on port 8088");
	}


}
