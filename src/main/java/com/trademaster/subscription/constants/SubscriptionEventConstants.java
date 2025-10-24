package com.trademaster.subscription.constants;

/**
 * Subscription Event Constants
 * MANDATORY: Rule #17 - All event type names centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all subscription event type names used throughout the system.
 * Prevents magic strings and ensures consistency across event publishing.
 *
 * @author TradeMaster Development Team
 */
public final class SubscriptionEventConstants {

    private SubscriptionEventConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Subscription Lifecycle Events
    public static final String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
    public static final String SUBSCRIPTION_ACTIVATED = "SUBSCRIPTION_ACTIVATED";
    public static final String SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
    public static final String SUBSCRIPTION_UPGRADED = "SUBSCRIPTION_UPGRADED";
    public static final String SUBSCRIPTION_DOWNGRADED = "SUBSCRIPTION_DOWNGRADED";
    public static final String SUBSCRIPTION_SUSPENDED = "SUBSCRIPTION_SUSPENDED";
    public static final String SUBSCRIPTION_RESUMED = "SUBSCRIPTION_RESUMED";
    public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";

    // Billing Events
    public static final String BILLING_SUCCESSFUL = "BILLING_SUCCESSFUL";
    public static final String BILLING_FAILED = "BILLING_FAILED";
    public static final String BILLING_RETRY = "BILLING_RETRY";

    // Trial Events
    public static final String TRIAL_STARTED = "TRIAL_STARTED";
    public static final String TRIAL_ENDING = "TRIAL_ENDING";
    public static final String TRIAL_ENDED = "TRIAL_ENDED";
    public static final String TRIAL_CONVERTED = "TRIAL_CONVERTED";

    // Usage Events
    public static final String USAGE_LIMIT_REACHED = "USAGE_LIMIT_REACHED";
    public static final String USAGE_LIMIT_EXCEEDED = "USAGE_LIMIT_EXCEEDED";
    public static final String USAGE_RESET = "USAGE_RESET";
}
