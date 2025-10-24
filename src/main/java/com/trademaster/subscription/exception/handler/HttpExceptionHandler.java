package com.trademaster.subscription.exception.handler;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.ErrorResponse;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

/**
 * HTTP Exception Handler
 * MANDATORY: Single Responsibility - HTTP protocol exceptions only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles HTTP protocol and routing exceptions.
 *
 * @author TradeMaster Development Team
 */
@RestControllerAdvice
@Slf4j
public class HttpExceptionHandler extends BaseExceptionHandler {

    public HttpExceptionHandler(StructuredLoggingService loggingService) {
        super(loggingService);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Method Not Allowed",
            String.format("Method %s not supported. Supported methods: %s",
                    ex.getMethod(), String.join(", ", ex.getSupportedMethods())),
            "METHOD_NOT_ALLOWED",
            request,
            Map.of("supportedMethods", ex.getSupportedMethods())
        );

        log.warn("Method not supported: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            String.format("No handler found for %s %s",
                    ex.getHttpMethod(), ex.getRequestURL()),
            "NO_HANDLER_FOUND",
            request,
            null
        );

        log.warn("No handler found: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
