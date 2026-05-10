package com.thriftybackpacker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger UI at http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI thriftyBackpackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ThriftyBackpacker API")
                        .description("Java Spring Boot backend — migrated from the Python FastAPI backend. " +
                                "All endpoints match the Python API contract used by the Vue 3 frontend. " +
                                "Swagger UI: http://localhost:8080/swagger-ui/index.html")
                        .version("0.1.0"));
    }
}
