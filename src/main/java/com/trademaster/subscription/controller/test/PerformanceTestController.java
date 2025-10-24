package com.trademaster.subscription.controller.test;

import com.trademaster.subscription.config.CorrelationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Performance Test Controller
 * MANDATORY: Single Responsibility - Performance and load testing only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles performance testing and health check endpoints.
 *
 * @author TradeMaster Development Team
 */
@RestController
@Slf4j
@Tag(name = "Performance Test Endpoints", description = "Performance testing and health check endpoints")
public class PerformanceTestController {

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

        try {
            Thread.sleep(10);
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
