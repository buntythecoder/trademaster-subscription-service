package com.trademaster.subscription.client;

/**
 * Internal Service Exception
 * MANDATORY: Single Responsibility - Exception data structure only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Exception thrown when internal service API calls fail.
 * Includes HTTP status code and correlation ID for tracing.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class InternalServiceException extends Exception {
    private final int statusCode;
    private final String correlationId;

    public InternalServiceException(String message, int statusCode, String correlationId) {
        super(message);
        this.statusCode = statusCode;
        this.correlationId = correlationId;
    }

    public InternalServiceException(String message, int statusCode, String correlationId, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.correlationId = correlationId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
