package com.trademaster.subscription.exception.handler;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.ErrorResponse;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Security Exception Handler
 * MANDATORY: Single Responsibility - Security exceptions only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles authentication and authorization exceptions.
 *
 * @author TradeMaster Development Team
 */
@RestControllerAdvice
@Slf4j
public class SecurityExceptionHandler extends BaseExceptionHandler {

    public SecurityExceptionHandler(StructuredLoggingService loggingService) {
        super(loggingService);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Authentication failed",
            "AUTHENTICATION_FAILED",
            request,
            null
        );

        log.warn("Authentication failed: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "Access denied - insufficient privileges",
            "ACCESS_DENIED",
            request,
            null
        );

        log.warn("Access denied: {} (correlationId: {}, userId: {})",
                ex.getMessage(),
                CorrelationConfig.CorrelationContext.getCorrelationId(),
                CorrelationConfig.CorrelationContext.getUserId());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}
