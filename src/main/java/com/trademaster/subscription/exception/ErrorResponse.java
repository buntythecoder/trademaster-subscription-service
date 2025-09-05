package com.trademaster.subscription.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Error Response DTO
 * 
 * Standardized error response structure for all API endpoints.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Error timestamp
     */
    private Instant timestamp;

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * HTTP status text
     */
    private String error;

    /**
     * Error message
     */
    private String message;

    /**
     * Application-specific error code
     */
    private String errorCode;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Field-level validation errors
     */
    private Map<String, List<String>> fieldErrors;

    /**
     * Additional error details
     */
    private Map<String, Object> details;
}