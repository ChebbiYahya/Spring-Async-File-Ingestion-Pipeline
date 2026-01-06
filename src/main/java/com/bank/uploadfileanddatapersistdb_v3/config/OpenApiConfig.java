package com.bank.uploadfileanddatapersistdb_v3.config;
// Configuration OpenAPI/Swagger pour documenter l'API.

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration.
 * Swagger UI: http://localhost:8080/api/swagger-ui/index.html#/
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring File Ingestion Pipeline API")
                        .version("1.0.0")
                        .description("CSV/XML ingestion with YAML validation, duplicate checks, persistence, and import logs."));
    }
}
