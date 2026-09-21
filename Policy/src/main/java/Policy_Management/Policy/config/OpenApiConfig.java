package Policy_Management.Policy.config;

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
    public OpenAPI policyOpenAPI() {
        return buildOpenApi(new Info()
                .title("Policy Management API")
                .version("v1")
                .description("APIs for Policy Management")
                .contact(new Contact().name("PMS Team").email("support@pms.com"))
                .license(new License().name("Internal Use")));
    }
}
