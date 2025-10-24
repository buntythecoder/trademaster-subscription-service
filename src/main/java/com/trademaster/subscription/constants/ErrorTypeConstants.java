package com.trademaster.subscription.constants;

/**
 * Error Type Constants
 * MANDATORY: Rule #17 - Constants & Magic Numbers
 *
 * Centralized error type definitions for exception handling and error tracking.
 * Used for categorizing errors, logging, and metrics collection.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class ErrorTypeConstants {

    // Validation Errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";

    // Business Logic Errors
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String INSUFFICIENT_PRIVILEGES = "INSUFFICIENT_PRIVILEGES";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";

    // System Errors
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    // Security Errors
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED";
    public static final String SECURITY_INCIDENT = "SECURITY_INCIDENT";

    // Rate Limiting Errors
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";

    private ErrorTypeConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
