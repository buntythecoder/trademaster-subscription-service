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
 * OpenAPI Configuration
 * 
 * Configures OpenAPI/Swagger documentation for the Subscription Service API.
 * Provides comprehensive API documentation with authentication and examples.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.api.base-url:http://localhost:8082}")
    private String baseUrl;

    @Bean
    public OpenAPI subscriptionServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url(baseUrl)
                    .description("Subscription Service API Server"),
                new Server()
                    .url("https://api.trademaster.com")
                    .description("Production API Server"),
                new Server()
                    .url("https://staging-api.trademaster.com")
                    .description("Staging API Server")
            ))
            .components(apiComponents())
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .addSecurityItem(new SecurityRequirement().addList("API Key Authentication"));
    }

    private Info apiInfo() {
        return new Info()
            .title("TradeMaster Subscription Service API")
            .description("""
                ## Overview
                The TradeMaster Subscription Service provides comprehensive subscription management capabilities including:
                
                - **Subscription Lifecycle Management**: Create, activate, upgrade, cancel, and suspend subscriptions
                - **Usage Tracking & Limits**: Real-time usage monitoring with tier-based limits
                - **Billing Integration**: Automated recurring billing with payment gateway integration
                - **Event-Driven Architecture**: Kafka-based events for system integration
                - **Multi-Tier Support**: FREE, PRO, AI PREMIUM, and INSTITUTIONAL tiers
                
                ## Features
                - ✅ Multi-tier subscription system with flexible pricing
                - ✅ Real-time usage tracking and limit enforcement  
                - ✅ Automated recurring billing with retry logic
                - ✅ Trial period management with expiration handling
                - ✅ Event publishing for system integration
                - ✅ Comprehensive metrics and monitoring
                - ✅ PCI DSS compliant payment processing
                
                ## Subscription Tiers
                
                ### FREE Tier
                - **Price**: $0/month
                - **API Calls**: 1,000/month
                - **Portfolios**: 3
                - **Watchlists**: 5
                - **Alerts**: 10
                - **Basic Analytics**: ✅
                
                ### PRO Tier
                - **Price**: $29.99/month ($299.99/year)
                - **API Calls**: 10,000/month
                - **Portfolios**: 10
                - **Watchlists**: 25
                - **Alerts**: 100
                - **Advanced Analytics**: ✅
                - **Data Export**: ✅
                
                ### AI PREMIUM Tier
                - **Price**: $99.99/month ($899.99/year)
                - **API Calls**: 50,000/month
                - **Portfolios**: 50
                - **Watchlists**: 100
                - **Alerts**: 500
                - **AI Insights**: 1,000/month
                - **Priority Support**: ✅
                
                ### INSTITUTIONAL Tier
                - **Price**: Custom pricing
                - **Unlimited Usage**: All features
                - **Dedicated Support**: ✅
                - **SLA Guarantees**: ✅
                - **Custom Integration**: ✅
                
                ## Usage Tracking
                Real-time usage tracking with automatic limit enforcement:
                - Feature-based usage monitoring
                - Monthly usage reset
                - Warning notifications at 80% limit
                - Graceful handling of limit exceeded scenarios
                
                ## Event System
                Publishes events for:
                - Subscription lifecycle changes
                - Usage limit violations
                - Billing events
                - Trial notifications
                
                ## Error Handling
                All endpoints return structured error responses with:
                - HTTP status codes
                - Error codes for programmatic handling
                - Detailed error messages
                - Field-level validation errors where applicable
                
                ## Rate Limiting
                API endpoints are rate limited based on subscription tier:
                - FREE: 100 requests/hour
                - PRO: 1,000 requests/hour  
                - AI PREMIUM: 5,000 requests/hour
                - INSTITUTIONAL: Unlimited
                """)
            .version(appVersion)
            .contact(new Contact()
                .name("TradeMaster Development Team")
                .email("dev@trademaster.com")
                .url("https://docs.trademaster.com"))
            .license(new License()
                .name("TradeMaster License")
                .url("https://trademaster.com/license"));
    }

    private Components apiComponents() {
        return new Components()
            .addSecuritySchemes("Bearer Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token obtained from authentication service"))
            .addSecuritySchemes("API Key Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key for service-to-service authentication"));
    }
}