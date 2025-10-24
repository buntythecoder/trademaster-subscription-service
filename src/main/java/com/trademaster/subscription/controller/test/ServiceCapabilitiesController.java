package com.trademaster.subscription.controller.test;

import com.trademaster.subscription.config.CorrelationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service Capabilities Controller
 * MANDATORY: Single Responsibility - Service capabilities and metadata only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Exposes subscription service capabilities and metadata for discovery.
 *
 * @author TradeMaster Development Team
 */
@RestController
@Slf4j
@Tag(name = "Service Capabilities", description = "Subscription service capabilities and metadata")
public class ServiceCapabilitiesController {

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
}
