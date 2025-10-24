package com.trademaster.subscription.event;

/**
 * Subscription Event Type Constants
 * MANDATORY: Single Responsibility - Event type definitions only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #18 - Constants in dedicated class
 *
 * Defines all subscription event type constants for Kafka routing.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public final class SubscriptionEventTypes {

    private SubscriptionEventTypes() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // Lifecycle Events
    public static final String SUBSCRIPTION_CREATED = "subscription.created";
    public static final String SUBSCRIPTION_ACTIVATED = "subscription.activated";
    public static final String SUBSCRIPTION_RENEWED = "subscription.renewed";
    public static final String SUBSCRIPTION_CANCELLED = "subscription.cancelled";
    public static final String SUBSCRIPTION_SUSPENDED = "subscription.suspended";
    public static final String SUBSCRIPTION_EXPIRED = "subscription.expired";

    // Tier Change Events
    public static final String SUBSCRIPTION_UPGRADED = "subscription.upgraded";
    public static final String SUBSCRIPTION_DOWNGRADED = "subscription.downgraded";

    // Payment Events
    public static final String SUBSCRIPTION_PAYMENT_FAILED = "subscription.payment.failed";
    public static final String SUBSCRIPTION_PAYMENT_RETRY = "subscription.payment.retry";

    // Usage Events
    public static final String USAGE_LIMIT_EXCEEDED = "subscription.usage.limit.exceeded";
    public static final String USAGE_WARNING_THRESHOLD = "subscription.usage.warning.threshold";

    // Trial Events
    public static final String TRIAL_STARTED = "subscription.trial.started";
    public static final String TRIAL_ENDING_SOON = "subscription.trial.ending.soon";
    public static final String TRIAL_EXPIRED = "subscription.trial.expired";
}
