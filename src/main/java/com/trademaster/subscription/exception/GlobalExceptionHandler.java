package com.trademaster.subscription.exception;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * 
 * Handles all exceptions across the subscription service with proper HTTP status codes
 * and structured error responses.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final StructuredLoggingService loggingService;

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
        
        Map<String, Object> usageDetails = new HashMap<>();
        if (ex.getUserId() != null) {
            usageDetails.put("userId", ex.getUserId());
        }
        if (ex.getFeatureName() != null) {
            usageDetails.put("feature", ex.getFeatureName());
        }
        if (ex.getCurrentUsage() != null) {
            usageDetails.put("currentUsage", ex.getCurrentUsage());
        }
        if (ex.getUsageLimit() != null) {
            usageDetails.put("usageLimit", ex.getUsageLimit());
        }

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            usageDetails
        );

        // Log security incident for usage limit violations
        loggingService.logSecurityIncident(
            "usage_limit_exceeded",
            "medium",
            ex.getUserId() != null ? ex.getUserId().toString() : null,
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
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            ex.hasFieldErrors() ? Map.of("fieldErrors", ex.getFieldErrors()) : null
        );
        
        // Also set fieldErrors directly on the response for backwards compatibility
        if (ex.hasFieldErrors()) {
            errorResponse.setFieldErrors(ex.getFieldErrors());
        }

        log.warn("Subscription validation error: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle method argument validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            "Invalid request data",
            "VALIDATION_ERROR",
            request,
            Map.of("validationErrors", fieldErrors)
        );
        
        // Set fieldErrors directly for backwards compatibility
        errorResponse.setFieldErrors(fieldErrors);

        log.warn("Method argument validation error: {} (correlationId: {})", 
                fieldErrors, CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            request,
            null
        );

        log.warn("Illegal argument: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            ex.getMessage(),
            "ILLEGAL_STATE",
            request,
            null
        );

        log.warn("Illegal state: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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

        // Log error with full context
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
     * Handle all other exceptions
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

        // Log error with full context
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

    /**
     * Handle authentication exceptions
     */
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

    /**
     * Handle access denied exceptions
     */
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

    /**
     * Handle method not supported exceptions
     */
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

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            String.format("Missing required parameter: %s (%s)", ex.getParameterName(), ex.getParameterType()),
            "MISSING_PARAMETER",
            request,
            Map.of("parameterName", ex.getParameterName(), "parameterType", ex.getParameterType())
        );

        log.warn("Missing parameter: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            message,
            "TYPE_MISMATCH",
            request,
            Map.of(
                "parameterName", ex.getName(),
                "providedValue", ex.getValue() != null ? ex.getValue().toString() : "null",
                "expectedType", ex.getRequiredType().getSimpleName()
            )
        );

        log.warn("Type mismatch: {} (correlationId: {})", 
                message, CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle no handler found exceptions
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
            "NO_HANDLER_FOUND",
            request,
            null
        );

        log.warn("No handler found: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Create standardized error response with correlation tracking
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String error, String message, 
                                            String errorCode, WebRequest request, Map<String, Object> additionalDetails) {
        
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        String requestId = CorrelationConfig.CorrelationContext.getRequestId();
        String userId = CorrelationConfig.CorrelationContext.getUserId();
        
        Map<String, Object> details = new HashMap<>();
        details.put("correlationId", correlationId);
        details.put("requestId", requestId);
        details.put("timestamp", Instant.now().toString());
        
        if (userId != null) {
            details.put("userId", userId);
        }
        
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }
        
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
     * Extract clean path from web request
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}