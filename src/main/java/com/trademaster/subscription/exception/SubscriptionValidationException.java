package com.trademaster.subscription.exception;

import java.util.List;
import java.util.Map;

/**
 * Subscription Validation Exception
 * 
 * Thrown when subscription data fails validation rules.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class SubscriptionValidationException extends SubscriptionException {

    private final Map<String, List<String>> fieldErrors;

    public SubscriptionValidationException(String message) {
        super(message, "SUBSCRIPTION_VALIDATION_ERROR");
        this.fieldErrors = null;
    }

    public SubscriptionValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, "SUBSCRIPTION_VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public SubscriptionValidationException(String message, Throwable cause) {
        super(message, "SUBSCRIPTION_VALIDATION_ERROR", cause);
        this.fieldErrors = null;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
}