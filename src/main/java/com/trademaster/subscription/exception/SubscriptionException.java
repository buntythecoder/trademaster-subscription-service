package com.trademaster.subscription.exception;

/**
 * Base Subscription Exception
 * 
 * Base exception class for all subscription-related errors.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public abstract class SubscriptionException extends RuntimeException {

    private final String errorCode;

    protected SubscriptionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected SubscriptionException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}