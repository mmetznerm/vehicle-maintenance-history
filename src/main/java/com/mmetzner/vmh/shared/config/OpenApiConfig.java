package com.mmetzner.vmh.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI vehicleMaintenanceHistoryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vehicle Maintenance History API")
                        .version("v1")
                        .description("API for managing vehicles and maintenance history."));
    }
}