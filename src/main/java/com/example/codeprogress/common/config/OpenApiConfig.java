package com.example.codeprogress.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CodeProgress API",
                version = "v1",
                description = "REST API for tracking programming topics, learning tasks, study sessions and progress."
        ),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH),
        tags = {
                @Tag(name = "Authentication", description = "Registration and access-token issuance"),
                @Tag(name = "Users", description = "Current authenticated user"),
                @Tag(name = "Topics", description = "Learning topic management and progress"),
                @Tag(name = "Learning tasks", description = "Tasks, planning and status tracking"),
                @Tag(name = "Study sessions", description = "Study history and completed tasks"),
                @Tag(name = "Dashboard", description = "Aggregated learning analytics"),
                @Tag(name = "Health", description = "Application health endpoint")
        }
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token returned by POST /api/auth/login"
)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
