package com.asohCloak.asohCloak.config.swaggerConfig;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Value("${API_VERSION:v1}")
    private String apiVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, bearerAuthScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("AsohCloak API")
                .description(
                        "Authentication & Authorization REST API secured entirely by Keycloak. "
                                + "Covers registration, login, OTP verification, magic-link login, "
                                + "password recovery, and account blocking/unblocking."
                )
                .version(apiVersion)
                .contact(new Contact()
                        .name("Asoh Yannick Anoh"));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the access token issued by Keycloak after login (without the \"Bearer \" prefix — Swagger adds it automatically).");
    }
}