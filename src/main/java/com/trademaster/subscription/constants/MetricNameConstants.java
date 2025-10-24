package com.trademaster.subscription.constants;

/**
 * Metric Name Constants
 * MANDATORY: Rule #17 - All Prometheus metric names centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all Prometheus metric names for subscription monitoring.
 * Ensures consistency across metric recording and dashboard queries.
 *
 * @author TradeMaster Development Team
 */
public final class MetricNameConstants {

    private MetricNameConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Counter Metrics
    public static final String SUBSCRIPTIONS_CREATED_TOTAL = "subscriptions_created_total";
    public static final String SUBSCRIPTIONS_ACTIVATED_TOTAL = "subscriptions_activated_total";
    public static final String SUBSCRIPTIONS_CANCELLED = "subscriptions.cancelled";
    public static final String SUBSCRIPTIONS_UPGRADED = "subscriptions.upgraded";
    public static final String SUBSCRIPTIONS_DOWNGRADED = "subscriptions.downgraded";
    public static final String BILLING_SUCCESSFUL = "billing.successful";
    public static final String BILLING_FAILED = "billing.failed";
    public static final String USAGE_LIMIT_EXCEEDED = "usage.limit_exceeded";
    public static final String TRIAL_CONVERSIONS_TOTAL = "trial_conversions_total";
    public static final String CHURN_EVENTS_TOTAL = "churn_events_total";
    public static final String PRICE_CHANGES_TOTAL = "price_changes_total";

    // Timer Metrics
    public static final String SUBSCRIPTION_PROCESSING_DURATION = "subscription_processing_duration_seconds";
    public static final String BILLING_PROCESSING_DURATION = "billing_processing_duration_seconds";
    public static final String USAGE_CHECK_DURATION = "usage_check_duration_seconds";
    public static final String SUBSCRIPTION_LIFETIME_DAYS = "subscription_lifetime_days";

    // Gauge Metrics
    public static final String ACTIVE_SUBSCRIPTIONS = "active_subscriptions";
    public static final String TRIAL_SUBSCRIPTIONS = "trial_subscriptions";
    public static final String SUSPENDED_SUBSCRIPTIONS = "suspended_subscriptions";
    public static final String MONTHLY_RECURRING_REVENUE = "monthly_recurring_revenue";
    public static final String ANNUAL_RECURRING_REVENUE = "annual_recurring_revenue";

    // Metric Tags
    public static final String TAG_TIER = "tier";
    public static final String TAG_BILLING_CYCLE = "billing_cycle";
    public static final String TAG_OPERATION = "operation";
    public static final String TAG_STATUS = "status";
    public static final String TAG_ERROR_TYPE = "error_type";
    public static final String TAG_FEATURE = "feature";
    public static final String TAG_REASON = "reason";
    public static final String TAG_CONVERTED = "converted";
    public static final String TAG_CHANGE_TYPE = "change_type";
    public static final String TAG_FROM_TIER = "from_tier";
    public static final String TAG_TO_TIER = "to_tier";
}
