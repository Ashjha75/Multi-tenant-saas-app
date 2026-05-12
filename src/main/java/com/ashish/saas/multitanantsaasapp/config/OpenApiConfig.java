package com.ashish.saas.multitanantsaasapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .schemaRequirement(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token obtained from the authentication endpoint")
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("ReputeAI SaaS Platform API")
                .description("""
                        Multi-tenant SaaS platform API for ReputeAI.
                        
                        This API provides endpoints for managing tenants, categories, and other platform resources.
                        All tenant-scoped operations require a valid JWT token in the Authorization header.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Ashish Jha")
                        .email("ashish@reputeai.com")
                )
                .license(new License()
                        .name("Proprietary")
                );
    }
}
