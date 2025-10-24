package com.trademaster.subscription.constants;

/**
 * Template Name Constants
 * MANDATORY: Rule #17 - All notification template names centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all notification template identifiers used for user communications.
 * Ensures consistency between subscription service and notification service.
 *
 * @author TradeMaster Development Team
 */
public final class TemplateNameConstants {

    private TemplateNameConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Subscription Lifecycle Templates
    public static final String SUBSCRIPTION_CREATED = "subscription_created";
    public static final String SUBSCRIPTION_ACTIVATED = "subscription_activated";
    public static final String SUBSCRIPTION_CANCELLED = "subscription_cancelled";
    public static final String SUBSCRIPTION_UPGRADED = "subscription_upgraded";
    public static final String SUBSCRIPTION_DOWNGRADED = "subscription_downgraded";
    public static final String SUBSCRIPTION_SUSPENDED = "subscription_suspended";
    public static final String SUBSCRIPTION_RESUMED = "subscription_resumed";
    public static final String SUBSCRIPTION_EXPIRED = "subscription_expired";

    // Billing Templates
    public static final String BILLING_SUCCESS = "billing_success";
    public static final String BILLING_FAILURE = "billing_failure";
    public static final String BILLING_RETRY = "billing_retry";
    public static final String BILLING_REMINDER = "billing_reminder";
    public static final String PAYMENT_METHOD_UPDATED = "payment_method_updated";

    // Trial Templates
    public static final String TRIAL_STARTED = "trial_started";
    public static final String TRIAL_ENDING = "trial_ending";
    public static final String TRIAL_ENDED = "trial_ended";
    public static final String TRIAL_CONVERTED = "trial_converted";

    // Usage Templates
    public static final String USAGE_LIMIT_WARNING = "usage_limit_warning";
    public static final String USAGE_LIMIT_REACHED = "usage_limit_reached";
    public static final String USAGE_LIMIT_EXCEEDED = "usage_limit_exceeded";

    // Tier Change Templates
    public static final String TIER_CHANGE_CONFIRMATION = "tier_change_confirmation";
    public static final String TIER_CHANGE_REMINDER = "tier_change_reminder";
}
