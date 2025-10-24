package com.trademaster.subscription.controller;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.security.SecurityContext;
import com.trademaster.subscription.security.SecurityFacade;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Greetings Controller for testing API key configuration
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 * Used to verify Kong Gateway authentication is working properly
 *
 * MANDATORY: Golden Specification - Kong API Gateway Integration
 * MANDATORY: Rule #6 - Zero Trust Security (Internal API endpoint)
 * MANDATORY: SecurityFacade for comprehensive audit trail
 *
 * @version 2.0.0
 */
@RestController
@RequestMapping("/internal/v1/greetings")
@Slf4j
public class GreetingsController {

    private final SecurityFacade securityFacade;

    public GreetingsController(SecurityFacade securityFacade) {
        this.securityFacade = securityFacade;
    }

    /**
     * Simple greeting endpoint for testing internal API key authentication
     * MANDATORY: Rule #6 - SecurityFacade for consistent audit trail
     * This endpoint should be protected by Kong API key authentication
     */
    @GetMapping("/hello")
    @PreAuthorize("hasRole('SERVICE')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> hello(HttpServletRequest httpRequest) {

        log.debug("Greetings endpoint accessed successfully");

        SecurityContext securityContext = buildSecurityContext(httpRequest);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("message", "Hello from TradeMaster Subscription Service!");
                response.put("service", "subscription-service");
                response.put("timestamp", LocalDateTime.now());
                response.put("status", "API key authentication working");
                response.put("version", "2.0.0");
                response.put("security-audit", "enabled");
                return CompletableFuture.completedFuture(Result.success(response));
            }
        ).thenApply(result -> result.match(
            response -> ResponseEntity.ok(response),
            securityError -> {
                Map<String, Object> error = new java.util.HashMap<>();
                error.put("error", securityError.message());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        ));
    }

    /**
     * Health check endpoint for internal monitoring
     * MANDATORY: Rule #6 - SecurityFacade for consistent audit trail
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('SERVICE')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> health(HttpServletRequest httpRequest) {

        log.debug("Internal health check accessed");

        SecurityContext securityContext = buildSecurityContext(httpRequest);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("status", "UP");
                response.put("service", "subscription-service");
                response.put("timestamp", LocalDateTime.now());
                response.put("authentication", "API key verified");
                response.put("security-audit", "enabled");
                return CompletableFuture.completedFuture(Result.success(response));
            }
        ).thenApply(result -> result.match(
            response -> ResponseEntity.ok(response),
            securityError -> {
                Map<String, Object> error = new java.util.HashMap<>();
                error.put("error", securityError.message());
                error.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        ));
    }

    /**
     * Build SecurityContext from HTTP request
     * MANDATORY: Rule #6 - Zero Trust Security Context for internal endpoints
     */
    private SecurityContext buildSecurityContext(HttpServletRequest httpRequest) {
        return SecurityContext.builder()
            .userId(UUID.fromString("00000000-0000-0000-0000-000000000003")) // Greetings/Test System ID
            .sessionId(httpRequest.getSession().getId())
            .ipAddress(httpRequest.getRemoteAddr())
            .userAgent(httpRequest.getHeader("User-Agent"))
            .requestPath(httpRequest.getRequestURI())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
