package com.trademaster.subscription.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation Configuration
 * 
 * Implements request correlation tracking for distributed tracing.
 * Ensures all logs and errors include correlation IDs for debugging.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class CorrelationConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * Correlation ID filter for request tracking
     */
    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.setName("correlationFilter");
        return registrationBean;
    }

    /**
     * Correlation Filter Implementation
     */
    public static class CorrelationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            
            String correlationId = extractCorrelationId(request);
            String requestId = UUID.randomUUID().toString();
            String userId = extractUserId(request);
            
            // Set MDC context for logging
            MDC.put("correlationId", correlationId);
            MDC.put("requestId", requestId);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());
            MDC.put("remoteAddr", getClientIpAddress(request));
            
            if (userId != null) {
                MDC.put("userId", userId);
            }
            
            // Set response headers
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            
            try {
                // Store in thread-local for access in services
                CorrelationContext.setCorrelationId(correlationId);
                CorrelationContext.setRequestId(requestId);
                CorrelationContext.setUserId(userId);
                
                log.debug("Request started: {} {} (correlationId: {}, requestId: {})",
                        request.getMethod(), request.getRequestURI(), correlationId, requestId);
                
                filterChain.doFilter(request, response);
                
                log.debug("Request completed: {} {} (correlationId: {}, status: {})",
                        request.getMethod(), request.getRequestURI(), correlationId, response.getStatus());
                
            } finally {
                // Clean up MDC and thread-local
                MDC.clear();
                CorrelationContext.clear();
            }
        }

        private String extractCorrelationId(HttpServletRequest request) {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            return correlationId;
        }

        private String extractUserId(HttpServletRequest request) {
            String userId = request.getHeader(USER_ID_HEADER);
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

    /**
     * Thread-local correlation context
     */
    public static class CorrelationContext {
        private static final ThreadLocal<String> correlationId = new ThreadLocal<>();
        private static final ThreadLocal<String> requestId = new ThreadLocal<>();
        private static final ThreadLocal<String> userId = new ThreadLocal<>();

        public static String getCorrelationId() {
            return correlationId.get();
        }

        public static void setCorrelationId(String id) {
            correlationId.set(id);
        }

        public static String getRequestId() {
            return requestId.get();
        }

        public static void setRequestId(String id) {
            requestId.set(id);
        }

        public static String getUserId() {
            return userId.get();
        }

        public static void setUserId(String id) {
            userId.set(id);
        }

        public static void clear() {
            correlationId.remove();
            requestId.remove();
            userId.remove();
        }
    }
}