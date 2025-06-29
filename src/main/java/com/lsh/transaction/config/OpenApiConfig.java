package com.banking.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration class for Swagger documentation.
 * Provides API documentation metadata and server information.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Management System API")
                        .description("RESTful API documentation for Banking Transaction Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("JoeyLiu")
                                .email("liush99@foxmail.com")
                                .url("https://github.com/Liu-Shihao/transaction-demo"))
                        )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Environment")
                ));
    }
} 