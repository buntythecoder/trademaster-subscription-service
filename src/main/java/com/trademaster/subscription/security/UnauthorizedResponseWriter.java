package com.trademaster.subscription.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Unauthorized Response Writer
 * MANDATORY: Single Responsibility - HTTP error response formatting only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Writes structured JSON error responses for authentication failures.
 * Includes correlation ID tracking and proper HTTP status codes.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class UnauthorizedResponseWriter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SERVICE_NAME = "subscription-service";
    private static final String ERROR_PATH = "internal-api";

    /**
     * Send unauthorized response with structured error format
     *
     * @param response      HTTP response
     * @param message       Error message
     * @param correlationId Request correlation ID
     * @throws IOException If I/O error occurs
     */
    public void sendUnauthorizedResponse(HttpServletResponse response, String message, String correlationId)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Add correlation ID to response headers
        // MANDATORY: Rule #3 - No if-else, using Optional pattern
        Optional.ofNullable(correlationId)
            .filter(StringUtils::hasText)
            .ifPresent(id -> response.setHeader(CORRELATION_ID_HEADER, id));

        String errorResponse = buildErrorResponse(message, correlationId);
        response.getWriter().write(errorResponse);

        log.warn("Sent unauthorized response - Message: {}, Correlation: {}", message, correlationId);
    }

    /**
     * Build structured JSON error response
     * MANDATORY: Rule #3 - No ternary operators, using Optional pattern
     *
     * @param message       Error message
     * @param correlationId Request correlation ID
     * @return JSON error response string
     */
    private String buildErrorResponse(String message, String correlationId) {
        String safeCorrelationId = Optional.ofNullable(correlationId)
            .filter(StringUtils::hasText)
            .orElse("none");

        return String.format(
            """
            {
              "error": "SERVICE_AUTHENTICATION_FAILED",
              "message": "%s",
              "timestamp": %d,
              "correlationId": "%s",
              "service": "%s",
              "path": "%s"
            }
            """,
            message,
            System.currentTimeMillis(),
            safeCorrelationId,
            SERVICE_NAME,
            ERROR_PATH
        );
    }
}
