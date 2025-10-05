package com.trademaster.subscription.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Kong API Gateway Service Authentication Filter
 *
 * MANDATORY implementation following TradeMaster Golden Specification.
 * Handles Kong API key authentication for internal service-to-service communication.
 *
 * Authentication Flow:
 * 1. Kong Gateway validates API key and sets consumer headers
 * 2. Filter validates Kong consumer headers (primary)
 * 3. Fallback to direct API key validation (secondary)
 * 4. Sets Spring Security authentication context
 *
 * NOTE: Registered in SecurityConfig.filterChain() AND as @Component.
 * @Component required for dependency injection. Do NOT add @Order to avoid double registration.
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
 * @since 2025-01-09
 */
@Component
@Slf4j
public class ServiceApiKeyFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String INTERNAL_API_PATH = "/api/internal/";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";

    @Value("${trademaster.security.service.api-key:pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q}")
    private String fallbackServiceApiKey;

    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;

    /**
     * Filter internal API requests for Kong API key authentication
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @param chain    Filter chain
     * @throws IOException      If I/O error occurs
     * @throws ServletException If servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Get correlation ID from CorrelationContext (set by CorrelationFilter)
        String correlationId = com.trademaster.subscription.config.CorrelationConfig.CorrelationContext.getCorrelationId();

        log.debug("Processing request: {} with correlation ID: {}", requestPath, correlationId);

        // Only process internal API requests
        if (!requestPath.startsWith(INTERNAL_API_PATH)) {
            log.trace("Skipping non-internal API request: {}", requestPath);
            chain.doFilter(request, response);
            return;
        }

        // Skip authentication if disabled (for development)
        if (!serviceAuthEnabled) {
            log.warn("Service authentication disabled - allowing request: {}", requestPath);
            setServiceAuthentication("dev-service");
            chain.doFilter(request, response);
            return;
        }

        // Check for Kong consumer headers first (Kong validated API key)
        String kongConsumerId = httpRequest.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = httpRequest.getHeader(KONG_CONSUMER_USERNAME_HEADER);

        // If Kong consumer headers present, Kong has validated the API key
        if (StringUtils.hasText(kongConsumerId) && StringUtils.hasText(kongConsumerUsername)) {
            log.info("Kong validated consumer '{}' (ID: {}) for correlation: {}, granting SERVICE access",
                    kongConsumerUsername, kongConsumerId, correlationId);
            setServiceAuthentication(kongConsumerUsername);
            chain.doFilter(request, response);
            return;
        }

        // Fall back to direct API key validation
        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        String serviceId = httpRequest.getHeader(SERVICE_ID_HEADER);

        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing Kong consumer headers and X-API-Key header for: {} (correlation: {})",
                    requestPath, correlationId);
            sendUnauthorizedResponse(httpResponse, "Missing service API key or Kong consumer headers", correlationId);
            return;
        }

        // For Kong dynamic authentication, we don't validate the API key directly
        // Kong validates the API key and sets consumer headers
        if (StringUtils.hasText(fallbackServiceApiKey)) {
            // Only validate if fallback key is configured
            if (!fallbackServiceApiKey.equals(apiKey)) {
                log.error("Invalid API key for direct service request: {} (correlation: {}, service: {})",
                        requestPath, correlationId, serviceId);
                sendUnauthorizedResponse(httpResponse, "Invalid service API key", correlationId);
                return;
            }
        } else {
            // No fallback key configured - this is expected for Kong dynamic auth
            log.info("Direct API key request with Kong dynamic authentication (no fallback validation): {} (correlation: {})",
                    requestPath, correlationId);
        }

        // Set authentication for direct service call
        String authenticationName = StringUtils.hasText(serviceId) ? serviceId : "direct-service-call";
        setServiceAuthentication(authenticationName);
        log.info("Direct API key authentication successful for: {} (correlation: {}, service: {})",
                requestPath, correlationId, serviceId);

        chain.doFilter(request, response);
    }

    /**
     * Set Spring Security authentication context for service calls
     *
     * @param serviceId Calling service identifier
     */
    private void setServiceAuthentication(String serviceId) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_SERVICE"),
            new SimpleGrantedAuthority("ROLE_INTERNAL")
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(serviceId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set SERVICE authentication for: {} with authorities: {}", serviceId, authorities);
    }

    /**
     * Send unauthorized response with structured error format
     *
     * @param response      HTTP response
     * @param message       Error message
     * @param correlationId Request correlation ID
     * @throws IOException If I/O error occurs
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message, String correlationId)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Add correlation ID to response headers
        if (StringUtils.hasText(correlationId)) {
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
        }

        String errorResponse = String.format(
            """
            {
              "error": "SERVICE_AUTHENTICATION_FAILED",
              "message": "%s",
              "timestamp": %d,
              "correlationId": "%s",
              "service": "subscription-service",
              "path": "internal-api"
            }
            """,
            message,
            System.currentTimeMillis(),
            correlationId != null ? correlationId : "none"
        );

        response.getWriter().write(errorResponse);
        log.warn("Sent unauthorized response - Message: {}, Correlation: {}", message, correlationId);
    }
}