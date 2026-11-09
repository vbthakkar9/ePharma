package com.pharmacy.saas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
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
    private String serverPort;

    @Value("${spring.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "sessionAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Pharmacy Management SaaS API")
                .version("1.0.0")
                .description("Multi-tenant Pharmacy Management System API Documentation")
                .license(new License().name("Proprietary").url("")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort + contextPath).description("Development Server"),
                new Server().url("https://api.pharmacy-saas.com" + contextPath).description("Production Server")
            ))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name("SESSION")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .description("Session cookie for authentication"))
            );
    }
}