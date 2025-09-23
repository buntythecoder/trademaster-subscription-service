package com.trademaster.subscription.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Documentation Configuration
 *
 * MANDATORY implementation following TradeMaster Golden Specification.
 * Provides comprehensive API documentation with enterprise features.
 *
 * Features:
 * - Multi-environment server configuration
 * - JWT and API key authentication schemes
 * - Comprehensive API descriptions with SLA information
 * - Kong Gateway integration documentation
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
 * @since 2025-01-09
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/subscription}")
    private String contextPath;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI subscriptionServiceOpenAPI() {
        return new OpenAPI()
            .info(buildApiInfo())
            .servers(buildServerList())
            .addSecurityItem(buildSecurityRequirement())
            .components(buildSecurityComponents());
    }

    private Info buildApiInfo() {
        return new Info()
            .title("TradeMaster Subscription Service API")
            .version("1.0.0")
            .description("""
                ## TradeMaster Subscription Service

                **Production-ready subscription management service with enterprise features**

                ### Core Features
                - **Subscription Lifecycle**: Complete management with SLA monitoring
                - **Usage Tracking**: Real-time tracking with performance metrics
                - **Multi-Tier Support**: FREE, PRO, AI Premium, and Institutional tiers
                - **Circuit Breaker Protection**: Resilient external service integration
                - **Performance Monitoring**: Real-time SLA compliance tracking

                ### Architecture
                - **Java 24 + Virtual Threads**: High-concurrency processing
                - **Functional Programming**: Zero if-else patterns, Result types
                - **Zero Trust Security**: JWT authentication with role-based access
                - **Consul Service Discovery**: Dynamic service registration
                - **Kong API Gateway**: API key authentication for internal services

                ### SLA Targets
                - **Critical Operations**: ≤25ms processing time
                - **High Priority**: ≤50ms processing time
                - **Standard Operations**: ≤100ms processing time
                - **Background Tasks**: ≤500ms processing time

                ### Monitoring & Observability
                - Prometheus metrics at `/actuator/prometheus`
                - Health checks at `/actuator/health`
                - Circuit breaker status at `/api/v1/subscription/circuit-breakers/status`

                ### Internal API Authentication
                Internal service endpoints (`/api/internal/*`) require Kong API key authentication
                with `X-API-Key` header and proper consumer configuration.
                """)
            .contact(buildContactInfo())
            .license(buildLicenseInfo());
    }

    private Contact buildContactInfo() {
        return new Contact()
            .name("TradeMaster Engineering Team")
            .email("engineering@trademaster.com")
            .url("https://trademaster.com/support");
    }

    private License buildLicenseInfo() {
        return new License()
            .name("TradeMaster Enterprise License")
            .url("https://trademaster.com/license");
    }

    private List<Server> buildServerList() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Development Environment"),
            new Server()
                .url("https://api-dev.trademaster.com" + contextPath)
                .description("Development Environment (Remote)"),
            new Server()
                .url("https://api-staging.trademaster.com" + contextPath)
                .description("Staging Environment"),
            new Server()
                .url("https://api.trademaster.com" + contextPath)
                .description("Production Environment")
        );
    }

    private SecurityRequirement buildSecurityRequirement() {
        return new SecurityRequirement().addList("Bearer Authentication");
    }

    private Components buildSecurityComponents() {
        return new Components()
            .addSecuritySchemes("Bearer Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("""
                        ### JWT Authentication

                        **Required for all authenticated endpoints**

                        #### How to obtain a token:
                        1. Authenticate with TradeMaster Authentication Service
                        2. Extract JWT token from the response
                        3. Include token in Authorization header: `Bearer <token>`

                        #### Token Structure:
                        - **Issuer**: TradeMaster Authentication Service
                        - **Expiration**: 1 hour (configurable)
                        - **Claims**: user ID, roles, permissions

                        #### Internal Service Authentication:
                        Internal endpoints use Kong API key authentication:
                        ```
                        X-API-Key: [service-api-key]
                        ```
                        """))
            .addSecuritySchemes("API Key Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("""
                        ### API Key Authentication

                        **Required for internal service-to-service communication**

                        #### Kong Gateway Integration:
                        - Kong validates API keys and sets consumer headers
                        - Services recognize `X-Consumer-ID` and `X-Consumer-Username`
                        - Fallback to direct API key validation if needed

                        #### Usage:
                        ```
                        X-API-Key: [service-api-key]
                        X-Service-ID: [calling-service-name]
                        ```
                        """));
    }
}