package com.trademaster.subscription.events;

/**
 * Subscription Event Type Enum
 * MANDATORY: Immutability - TradeMaster Rule #9
 * MANDATORY: Pattern Matching - TradeMaster Rule #14
 *
 * Defines all possible subscription event types for notification system.
 *
 * @author TradeMaster Development Team
 */
public enum SubscriptionEventType {
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_ACTIVATED,
    SUBSCRIPTION_EXPIRED,
    SUBSCRIPTION_SUSPENDED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_UPGRADED,
    SUBSCRIPTION_DOWNGRADED,
    TRIAL_STARTED,
    TRIAL_ENDING,
    TRIAL_ENDED,
    BILLING_SUCCESS,
    BILLING_FAILURE,
    USAGE_LIMIT_EXCEEDED
}
