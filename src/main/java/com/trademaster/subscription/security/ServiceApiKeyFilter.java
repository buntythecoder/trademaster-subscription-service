package com.trademaster.subscription.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

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

    private static final String INTERNAL_API_PATH = "/api/internal/";

    private final ServiceAuthenticationHandler authenticationHandler;

    public ServiceApiKeyFilter(ServiceAuthenticationHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

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

        // MANDATORY: Rule #3 - No if-else, using functional guard pattern
        // Guard 1: Skip non-internal API requests
        Optional.of(!requestPath.startsWith(INTERNAL_API_PATH))
            .filter(Boolean::booleanValue)
            .ifPresentOrElse(
                skip -> {
                    log.trace("Skipping non-internal API request: {}", requestPath);
                    try { chain.doFilter(request, response); }
                    catch (IOException | ServletException e) { throw new RuntimeException(e); }
                },
                () -> authenticationHandler.processInternalRequest(httpRequest, httpResponse, chain, requestPath, correlationId)
            );
    }

}