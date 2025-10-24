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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Public Test Controller
 * MANDATORY: Single Responsibility - Public test endpoints only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles public test endpoints without authentication requirements.
 *
 * @author TradeMaster Development Team
 */
@RestController
@Slf4j
@Tag(name = "Public Test Endpoints", description = "Public Kong gateway test endpoints without authentication")
public class PublicTestController {

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
}
