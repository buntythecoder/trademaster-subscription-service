package com.trademaster.subscription.filter;

import com.trademaster.subscription.config.CorrelationConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Correlation ID filter that adds correlation IDs to all requests.
 * Must be the first filter in the chain for proper request tracking.
 *
 * Implementation notes:
 * - Uses @Component instead of FilterRegistrationBean to avoid ordering conflicts
 * - Implements Ordered interface for explicit ordering control
 * - Uses MDC for thread-local correlation ID storage
 * - Compatible with Java 24 Virtual Threads
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class CorrelationFilter implements Filter, Ordered {

    @Override
    public int getOrder() {
        // Highest precedence - must be first filter
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CorrelationFilter initialized with order: {}", getOrder());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        log.debug("CorrelationFilter processing: {} {}", method, requestUri);

        try {
            // Generate or extract correlation ID
            String correlationId = extractOrGenerateCorrelationId(httpRequest);
            String requestId = UUID.randomUUID().toString();
            String userId = extractUserId(httpRequest);

            // Set in MDC for logging
            MDC.put("correlationId", correlationId);
            MDC.put("requestId", requestId);
            MDC.put("method", method);
            MDC.put("uri", requestUri);
            MDC.put("remoteAddr", getClientIpAddress(httpRequest));

            // MANDATORY: Rule #3 - No if-else, using Optional
            Optional.ofNullable(userId).ifPresent(id -> MDC.put("userId", id));

            // Store in thread-local for access in services
            CorrelationConfig.CorrelationContext.setCorrelationId(correlationId);
            CorrelationConfig.CorrelationContext.setRequestId(requestId);
            CorrelationConfig.CorrelationContext.setUserId(userId);

            // Add to response headers
            httpResponse.setHeader(CorrelationConfig.CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader(CorrelationConfig.REQUEST_ID_HEADER, requestId);

            log.info("CorrelationFilter: Request {} {} assigned correlation ID: {}", method, requestUri, correlationId);

            // Verify the correlation ID was set correctly
            String verifyId = CorrelationConfig.CorrelationContext.getCorrelationId();
            log.info("CorrelationFilter: Verification - correlationId retrieved from context: {}", verifyId);

            // Continue filter chain
            chain.doFilter(request, response);

            log.debug("CorrelationFilter completed for: {} {} (correlationId: {})", method, requestUri, correlationId);

        } finally {
            // Always clean up MDC and thread-local
            MDC.clear();
            CorrelationConfig.CorrelationContext.clear();
        }
    }

    @Override
    public void destroy() {
        log.info("CorrelationFilter destroyed");
    }

    /**
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(CorrelationConfig.CORRELATION_ID_HEADER))
            .filter(id -> !id.trim().isEmpty())
            .map(id -> {
                log.debug("Using existing correlation ID from header: {}", id);
                return id;
            })
            .orElseGet(() -> {
                String newId = UUID.randomUUID().toString();
                log.debug("Generated new correlation ID: {}", newId);
                return newId;
            });
    }

    /**
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    private String extractUserId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(CorrelationConfig.USER_ID_HEADER))
            .filter(id -> !id.trim().isEmpty())
            .orElse(null); // Will be set by security layer
    }

    /**
     * MANDATORY: Rule #3 - No if-else, using Optional chaining
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(header -> !header.isEmpty())
            .map(header -> header.split(",")[0].trim())
            .or(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .filter(header -> !header.isEmpty()))
            .orElseGet(request::getRemoteAddr);
    }
}