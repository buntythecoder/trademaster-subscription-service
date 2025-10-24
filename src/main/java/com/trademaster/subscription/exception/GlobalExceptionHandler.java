package com.trademaster.subscription.exception;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.handler.BaseExceptionHandler;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Global Exception Handler
 * MANDATORY: Single Responsibility - Catch-all exception handling only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles catch-all exceptions that don't fit into specific categories.
 * Domain exceptions → DomainExceptionHandler
 * Web/validation/security exceptions → WebValidationExceptionHandler
 * Generic runtime/unexpected exceptions → This handler
 *
 * @author TradeMaster Development Team
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends BaseExceptionHandler {

    public GlobalExceptionHandler(StructuredLoggingService loggingService) {
        super(loggingService);
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            request,
            Map.of("exceptionType", ex.getClass().getSimpleName())
        );

        loggingService.logError(
            "runtime_exception",
            ex.getMessage(),
            "RUNTIME_EXCEPTION",
            ex,
            Map.of(
                "path", extractPath(request),
                "correlationId", CorrelationConfig.CorrelationContext.getCorrelationId(),
                "requestId", CorrelationConfig.CorrelationContext.getRequestId()
            )
        );

        log.error("Unexpected runtime exception (correlationId: {}): ",
                CorrelationConfig.CorrelationContext.getCorrelationId(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            request,
            Map.of("exceptionType", ex.getClass().getSimpleName())
        );

        loggingService.logError(
            "generic_exception",
            ex.getMessage(),
            "GENERIC_EXCEPTION",
            ex,
            Map.of(
                "path", extractPath(request),
                "correlationId", CorrelationConfig.CorrelationContext.getCorrelationId(),
                "requestId", CorrelationConfig.CorrelationContext.getRequestId()
            )
        );

        log.error("Unexpected exception (correlationId: {}): ",
                CorrelationConfig.CorrelationContext.getCorrelationId(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}