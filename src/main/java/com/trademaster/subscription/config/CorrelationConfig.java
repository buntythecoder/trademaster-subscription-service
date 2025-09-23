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