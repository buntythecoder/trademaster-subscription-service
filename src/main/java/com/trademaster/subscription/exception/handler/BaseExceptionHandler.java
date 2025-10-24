package com.trademaster.subscription.exception.handler;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.ErrorResponse;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base Exception Handler
 * MANDATORY: DRY Principle - Shared exception handling infrastructure
 * MANDATORY: Single Responsibility - Common error response building only
 *
 * Provides shared helper methods for all exception handlers.
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
public abstract class BaseExceptionHandler {

    protected final StructuredLoggingService loggingService;

    /**
     * Create standardized error response with correlation tracking
     */
    protected ErrorResponse createErrorResponse(HttpStatus status, String error, String message,
                                               String errorCode, WebRequest request, Map<String, Object> additionalDetails) {

        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        String requestId = CorrelationConfig.CorrelationContext.getRequestId();
        String userId = CorrelationConfig.CorrelationContext.getUserId();

        Map<String, Object> details = new HashMap<>();
        details.put("correlationId", correlationId);
        details.put("requestId", requestId);
        details.put("timestamp", Instant.now().toString());

        // MANDATORY: Rule #3 - No if-else, using Optional pattern
        java.util.Optional.ofNullable(userId)
            .ifPresent(id -> details.put("userId", id));

        java.util.Optional.ofNullable(additionalDetails)
            .ifPresent(details::putAll);

        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .path(extractPath(request))
                .details(details)
                .build();
    }

    /**
     * Extract request path from WebRequest
     */
    protected String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
