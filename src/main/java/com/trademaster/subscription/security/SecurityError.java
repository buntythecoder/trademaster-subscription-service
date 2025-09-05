package com.trademaster.subscription.security;

/**
 * Security Error for External Access Violations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SecurityError(
    String message,
    SecurityErrorType type,
    long timestamp
) {
    
    public SecurityError(String message) {
        this(message, SecurityErrorType.GENERAL, System.currentTimeMillis());
    }
    
    public SecurityError(String message, SecurityErrorType type) {
        this(message, type, System.currentTimeMillis());
    }
    
    public enum SecurityErrorType {
        AUTHENTICATION_FAILED,
        AUTHORIZATION_DENIED,
        RISK_ASSESSMENT_FAILED,
        RATE_LIMIT_EXCEEDED,
        GENERAL
    }
}