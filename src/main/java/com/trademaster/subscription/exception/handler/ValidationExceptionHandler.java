package com.trademaster.subscription.exception.handler;

import com.trademaster.subscription.config.CorrelationConfig;
import com.trademaster.subscription.exception.ErrorResponse;
import com.trademaster.subscription.service.StructuredLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validation Exception Handler
 * MANDATORY: Single Responsibility - Input validation exceptions only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles request validation and argument exceptions.
 *
 * @author TradeMaster Development Team
 */
@RestControllerAdvice
@Slf4j
public class ValidationExceptionHandler extends BaseExceptionHandler {

    public ValidationExceptionHandler(StructuredLoggingService loggingService) {
        super(loggingService);
    }

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

        errorResponse.setFieldErrors(fieldErrors);

        log.warn("Method argument validation error: {} (correlationId: {})",
                fieldErrors, CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            String.format("Missing required parameter: %s (%s)",
                    ex.getParameterName(), ex.getParameterType()),
            "MISSING_PARAMETER",
            request,
            Map.of("parameterName", ex.getParameterName(),
                   "parameterType", ex.getParameterType())
        );

        log.warn("Missing parameter: {} (correlationId: {})",
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

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
}
