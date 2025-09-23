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

            if (userId != null) {
                MDC.put("userId", userId);
            }

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

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // Try to extract from header first
        String correlationId = request.getHeader(CorrelationConfig.CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            // Generate new correlation ID
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID from header: {}", correlationId);
        }

        return correlationId;
    }

    private String extractUserId(HttpServletRequest request) {
        String userId = request.getHeader(CorrelationConfig.USER_ID_HEADER);
        if (userId == null || userId.trim().isEmpty()) {
            // Try to extract from JWT token or other auth mechanisms
            // For now, return null - will be set by security layer
            return null;
        }
        return userId;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}