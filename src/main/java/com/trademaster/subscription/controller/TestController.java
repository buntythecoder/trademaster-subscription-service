package com.trademaster.subscription.controller;

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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Test Controller for Kong API Gateway Integration
 *
 * MANDATORY: TradeMaster Kong Integration Test Endpoints
 * - External API endpoints with JWT authentication
 * - Internal API endpoints with API key authentication
 * - Kong routing validation endpoints
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@Slf4j
@Tag(name = "Test Endpoints", description = "Kong Gateway integration test endpoints")
public class TestController {

    /**
     * Public health endpoint - no authentication required
     */
    @GetMapping("/api/v1/test/ping")
    @Operation(
        summary = "Public ping endpoint",
        description = "Simple ping endpoint for basic connectivity testing"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is responding")
    })
    public ResponseEntity<Map<String, Object>> ping(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Ping request received from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId != null ? correlationId : "none",
            "requestId", UUID.randomUUID().toString()
        ));
    }

    /**
     * External API endpoint - requires JWT authentication
     */
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

    /**
     * Internal API endpoint - requires API key authentication
     */
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

        // Log Kong headers for debugging
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

    /**
     * Admin endpoint - requires elevated permissions
     */
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

    /**
     * Echo headers endpoint - for debugging Kong headers
     */
    @GetMapping("/api/v1/test/headers")
    @Operation(
        summary = "Echo request headers",
        description = "Returns all request headers for Kong routing debugging"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Headers returned successfully")
    })
    public ResponseEntity<Map<String, Object>> echoHeaders(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Headers echo request from: {} (correlationId: {})",
                request.getRemoteAddr(), correlationId);

        // Collect all headers
        Map<String, String> headers = new java.util.HashMap<>();
        java.util.Collections.list(request.getHeaderNames())
            .forEach(headerName ->
                headers.put(headerName, request.getHeader(headerName))
            );

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "headers", headers,
            "requestInfo", Map.of(
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "remoteAddr", request.getRemoteAddr(),
                "serverName", request.getServerName(),
                "serverPort", request.getServerPort()
            )
        ));
    }

    /**
     * API key validation testing endpoint
     */
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

        // Extract API key validation details
        String apiKey = request.getHeader("X-API-Key");
        String serviceId = request.getHeader("X-Service-ID");
        String kongConsumer = request.getHeader("X-Consumer-Username");
        String kongConsumerId = request.getHeader("X-Consumer-ID");

        // Log validation details for debugging
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

    /**
     * Subscription service capabilities endpoint
     */
    @GetMapping("/api/internal/capabilities")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(
        summary = "Service capabilities",
        description = "Returns subscription service capabilities and health status"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service capabilities returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - API key required")
    })
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        log.info("Service capabilities requested (correlationId: {})", correlationId);

        return ResponseEntity.ok(Map.of(
            "service", "subscription-service",
            "version", "1.0.0",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "capabilities", Map.of(
                "subscription_management", true,
                "billing_processing", true,
                "notification_support", true,
                "upgrade_downgrade", true,
                "trial_management", true,
                "subscription_history", true
            ),
            "endpoints", Map.of(
                "health_check", "/api/v2/health",
                "subscription_create", "/api/v1/subscriptions",
                "subscription_get", "/api/v1/subscriptions/{id}",
                "subscription_upgrade", "/api/v1/subscriptions/{id}/upgrade",
                "subscription_cancel", "/api/v1/subscriptions/{id}/cancel",
                "api_key_validation", "/api/internal/test/validate-api-key"
            ),
            "sla", Map.of(
                "target_response_time_ms", 100,
                "availability", "99.9%",
                "max_concurrent_users", 10000
            )
        ));
    }

    /**
     * Load test endpoint - for performance validation
     */
    @PostMapping("/api/v1/test/load")
    @Operation(
        summary = "Load test endpoint",
        description = "Simple endpoint for load testing Kong routing and service performance"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Load test request processed")
    })
    public ResponseEntity<Map<String, Object>> loadTest(
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request) {

        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        long startTime = System.nanoTime();

        // Simulate some processing
        try {
            Thread.sleep(10); // 10ms simulated processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long processingTime = System.nanoTime() - startTime;

        log.debug("Load test request processed in {}ns (correlationId: {})",
                processingTime, correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "processed",
            "service", "subscription-service",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "performance", Map.of(
                "processing_time_ns", processingTime,
                "processing_time_ms", processingTime / 1_000_000.0
            ),
            "payload_received", payload != null,
            "payload_size", payload != null ? payload.size() : 0
        ));
    }

    /**
     * Simple test endpoint without authorization for Kong testing
     */
    @GetMapping("/api/internal/test/kong-auth")
    @Operation(
        summary = "Kong authentication test",
        description = "Simple endpoint to test Kong consumer header injection without Spring Security authorization"
    )
    public ResponseEntity<Map<String, Object>> testKongAuth(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();

        // Get Kong consumer headers
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

    /**
     * Simple health check endpoint for API key validation
     */
    @GetMapping("/api/internal/health-check")
    @Operation(
        summary = "Simple health check with API key validation",
        description = "Basic endpoint to verify service is responding with API key"
    )
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request) {
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        String kongConsumer = request.getHeader("X-Consumer-Username");
        String kongConsumerId = request.getHeader("X-Consumer-ID");

        log.info("Health check - Consumer: {}, ID: {}, Correlation: {}",
                kongConsumer, kongConsumerId, correlationId);

        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "subscription-service",
            "version", "1.0.0",
            "timestamp", LocalDateTime.now(),
            "correlationId", correlationId,
            "authenticated", kongConsumer != null,
            "consumer", kongConsumer != null ? kongConsumer : "anonymous",
            "message", "Service is healthy and responding"
        ));
    }
}