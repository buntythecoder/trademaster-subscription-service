package com.trademaster.subscription.exception;

import java.util.UUID;

/**
 * Subscription Not Found Exception
 * 
 * Thrown when a requested subscription does not exist.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class SubscriptionNotFoundException extends SubscriptionException {

    public SubscriptionNotFoundException(UUID subscriptionId) {
        super("Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND");
    }

    public SubscriptionNotFoundException(UUID userId, String context) {
        super("No active subscription found for user: " + userId + " (" + context + ")", "SUBSCRIPTION_NOT_FOUND");
    }

    public SubscriptionNotFoundException(String message) {
        super(message, "SUBSCRIPTION_NOT_FOUND");
    }
}