package com.ashish.saas.multitanantsaasapp.config;

import io.swagger.v3.oas.models.Components;
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

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String BEARER_AUTH = "BearerAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme tenantScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(TENANT_HEADER)
                .description("Tenant ID header");

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ))
                .components(new Components()
                        .addSecuritySchemes(TENANT_HEADER, tenantScheme)
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme)
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(TENANT_HEADER)
                                .addList(BEARER_AUTH)
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("ReputeAI SaaS Platform API")
                .version("1.0.0")
                .description("Multi-tenant SaaS platform APIs")
                .contact(new Contact()
                        .name("Ashish Jha")
                        .email("ashish@reputeai.com"))
                .license(new License().name("Proprietary"));
    }
}