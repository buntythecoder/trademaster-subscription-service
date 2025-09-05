package com.trademaster.subscription.exception;

import com.trademaster.subscription.enums.SubscriptionStatus;

import java.util.UUID;

/**
 * Invalid Subscription State Exception
 * 
 * Thrown when attempting an operation that is not valid for the current subscription state.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class InvalidSubscriptionStateException extends SubscriptionException {

    public InvalidSubscriptionStateException(UUID subscriptionId, SubscriptionStatus currentStatus, String operation) {
        super(String.format("Cannot perform operation '%s' on subscription %s in status '%s'", 
              operation, subscriptionId, currentStatus), 
              "INVALID_SUBSCRIPTION_STATE");
    }

    public InvalidSubscriptionStateException(String message) {
        super(message, "INVALID_SUBSCRIPTION_STATE");
    }

    public InvalidSubscriptionStateException(String message, Throwable cause) {
        super(message, "INVALID_SUBSCRIPTION_STATE", cause);
    }
}