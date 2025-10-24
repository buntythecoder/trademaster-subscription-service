package com.trademaster.subscription.controller.test;

import com.trademaster.subscription.config.CorrelationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Security Test Controller
 * MANDATORY: Single Responsibility - Security authentication testing only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles security testing endpoints with various authentication methods.
 *
 * @author TradeMaster Development Team
 */
@RestController
@Slf4j
@Tag(name = "Security Test Endpoints", description = "Authentication and authorization test endpoints")
public class SecurityTestController {

    @GetMapping("/api/v1/test/secure")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Secure test endpoint",
        description = "JWT authenticated endpoint for external access testing",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authenticated request successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> secureEndpoint(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Secure endpoint accessed from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "authenticated",
            "message", "JWT authentication successful",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "userInfo", Map.of(
                "authenticated", true,
                "authorities", request.getAttribute("authorities")
            )
        ));
    }

    @GetMapping("/api/internal/test/service-ping")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(
        summary = "Internal service ping",
        description = "API key authenticated endpoint for internal service-to-service testing",
        security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service authenticated request successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient service permissions")
    })
    public ResponseEntity<Map<String, Object>> servicePing(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Internal service ping from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        String kongConsumer = request.getHeader("X-Consumer-Username");
        String kongConsumerId = request.getHeader("X-Consumer-ID");

        log.debug("Kong consumer info - Username: {}, ID: {}", kongConsumer, kongConsumerId);

        return ResponseEntity.ok(Map.of(
            "status", "service_authenticated",
            "message", "API key authentication successful",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "kong", Map.of(
                "consumer", kongConsumer != null ? kongConsumer : "unknown",
                "consumerId", kongConsumerId != null ? kongConsumerId : "unknown"
            ),
            "serviceInfo", Map.of(
                "authenticated", true,
                "authorities", "ROLE_SERVICE"
            )
        ));
    }

    @GetMapping("/api/internal/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Admin test endpoint",
        description = "Admin authenticated endpoint for administrative testing"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Admin authenticated request successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<Map<String, Object>> adminEndpoint(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Admin endpoint accessed from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "admin_authenticated",
            "message", "Admin authentication successful",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "adminInfo", Map.of(
                "authenticated", true,
                "authorities", "ROLE_ADMIN"
            )
        ));
    }

    @GetMapping("/api/internal/test/validate-api-key")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(
        summary = "API key validation test",
        description = "Tests API key authentication for internal service communication validation",
        security = @SecurityRequirement(name = "apiKeyAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API key validation successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient service permissions")
    })
    public ResponseEntity<Map<String, Object>> validateApiKey(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("API key validation test from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        String apiKey = request.getHeader("X-API-Key");
        String serviceId = request.getHeader("X-Service-ID");
        String kongConsumer = request.getHeader("X-Consumer-Username");
        String kongConsumerId = request.getHeader("X-Consumer-ID");

        log.debug("API Key validation details - API-Key present: {}, Service-ID: {}, Kong-Consumer: {}, Kong-Consumer-ID: {}",
                apiKey != null, serviceId, kongConsumer, kongConsumerId);

        return ResponseEntity.ok(Map.of(
            "status", "api_key_validated",
            "message", "API key authentication validation successful",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "validation_details", Map.of(
                "api_key_present", apiKey != null,
                "api_key_length", apiKey != null ? apiKey.length() : 0,
                "service_id", serviceId != null ? serviceId : "not_provided",
                "kong_validated", kongConsumer != null && kongConsumerId != null,
                "kong_consumer", kongConsumer != null ? kongConsumer : "not_provided",
                "kong_consumer_id", kongConsumerId != null ? kongConsumerId : "not_provided"
            ),
            "authentication_flow", Map.of(
                "primary", "Kong consumer headers validation",
                "fallback", "Direct API key validation",
                "current_method", kongConsumer != null ? "kong_validated" : "direct_api_key"
            ),
            "security_context", Map.of(
                "authenticated", true,
                "authorities", "ROLE_SERVICE, ROLE_INTERNAL",
                "scopes", "subscription:read, subscription:write"
            )
        ));
    }

    @GetMapping("/api/internal/test/kong-auth")
    @Operation(
        summary = "Kong authentication test",
        description = "Simple endpoint to test Kong consumer header injection without Spring Security authorization"
    )
    public ResponseEntity<Map<String, Object>> testKongAuth(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        String kongConsumer = request.getHeader("X-Consumer-Username");
        String kongConsumerId = request.getHeader("X-Consumer-ID");
        String apiKey = request.getHeader("X-API-Key");

        log.info("Kong auth test - Consumer: {}, ID: {}, Correlation: {}",
                kongConsumer, kongConsumerId, correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Kong authentication test successful",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "kong_headers", Map.of(
                "consumer_username", kongConsumer != null ? kongConsumer : "not_present",
                "consumer_id", kongConsumerId != null ? kongConsumerId : "not_present",
                "api_key_present", apiKey != null
            ),
            "all_headers", request.getHeaderNames() != null ?
                java.util.Collections.list(request.getHeaderNames()).stream()
                    .collect(java.util.stream.Collectors.toMap(
                        h -> h,
                        h -> request.getHeader(h)
                    )) : Map.of()
        ));
    }
}
