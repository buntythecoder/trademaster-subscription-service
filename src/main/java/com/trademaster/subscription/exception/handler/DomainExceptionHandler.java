package com.trademaster.subscription.exception.handler;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.ErrorResponse;
import com.trademaster.subscription.exception.InvalidSubscriptionStateException;
import com.trademaster.subscription.exception.SubscriptionNotFoundException;
import com.trademaster.subscription.exception.SubscriptionValidationException;
import com.trademaster.subscription.exception.UsageLimitExceededException;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain Exception Handler
 * MANDATORY: Single Responsibility - Business/domain exceptions only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles subscription-specific business exceptions.
 *
 * @author TradeMaster Development Team
 */
@RestControllerAdvice
@Slf4j
public class DomainExceptionHandler extends BaseExceptionHandler {

    public DomainExceptionHandler(StructuredLoggingService loggingService) {
        super(loggingService);
    }

    /**
     * Handle subscription not found exceptions
     */
    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(
            SubscriptionNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            null
        );

        log.warn("Subscription not found: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle invalid subscription state exceptions
     */
    @ExceptionHandler(InvalidSubscriptionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSubscriptionState(
            InvalidSubscriptionStateException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            null
        );

        log.warn("Invalid subscription state: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle usage limit exceeded exceptions
     */
    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleUsageLimitExceeded(
            UsageLimitExceededException ex, WebRequest request) {

        // MANDATORY: Rule #3 - No if-else, using Optional pattern
        Map<String, Object> usageDetails = new HashMap<>();
        java.util.Optional.ofNullable(ex.getUserId())
            .ifPresent(userId -> usageDetails.put("userId", userId));
        java.util.Optional.ofNullable(ex.getFeatureName())
            .ifPresent(feature -> usageDetails.put("feature", feature));
        java.util.Optional.ofNullable(ex.getCurrentUsage())
            .ifPresent(current -> usageDetails.put("currentUsage", current));
        java.util.Optional.ofNullable(ex.getUsageLimit())
            .ifPresent(limit -> usageDetails.put("usageLimit", limit));

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            usageDetails
        );

        // Log security incident for usage limit violations
        // MANDATORY: Rule #3 - No ternary operators, using Optional pattern
        loggingService.logSecurityIncident(
            "usage_limit_exceeded",
            "medium",
            java.util.Optional.ofNullable(ex.getUserId())
                .map(Object::toString)
                .orElse(null),
            null,
            null,
            usageDetails
        );

        log.warn("Usage limit exceeded: {} (correlationId: {}, userId: {})",
                ex.getMessage(),
                CorrelationConfig.CorrelationContext.getCorrelationId(),
                ex.getUserId());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle subscription validation exceptions
     */
    @ExceptionHandler(SubscriptionValidationException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionValidation(
            SubscriptionValidationException ex, WebRequest request) {

        // MANDATORY: Rule #3 - No ternary/if-else, using Optional pattern
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            java.util.Optional.of(ex.hasFieldErrors())
                .filter(Boolean::booleanValue)
                .map(hasErrors -> java.util.Map.<String, Object>of("fieldErrors", ex.getFieldErrors()))
                .orElse(null)
        );

        // Also set fieldErrors directly on the response for backwards compatibility
        java.util.Optional.of(ex.hasFieldErrors())
            .filter(Boolean::booleanValue)
            .ifPresent(hasErrors -> errorResponse.setFieldErrors(ex.getFieldErrors()));

        log.warn("Subscription validation error: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
