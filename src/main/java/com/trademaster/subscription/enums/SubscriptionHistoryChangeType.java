package com.trademaster.subscription.enums;

/**
 * Subscription History Change Type
 * MANDATORY: Single Responsibility - Change type enumeration only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Change types for subscription history tracking.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum SubscriptionHistoryChangeType {
    CREATED("Subscription Created"),
    ACTIVATED("Subscription Activated"),
    UPGRADED("Tier Upgraded"),
    DOWNGRADED("Tier Downgraded"),
    BILLING_CYCLE_CHANGED("Billing Cycle Changed"),
    SUSPENDED("Subscription Suspended"),
    CANCELLED("Subscription Cancelled"),
    TERMINATED("Subscription Terminated"),
    REACTIVATED("Subscription Reactivated"),
    PAUSED("Subscription Paused"),
    RESUMED("Subscription Resumed"),
    TRIAL_STARTED("Trial Started"),
    TRIAL_ENDED("Trial Ended"),
    PAYMENT_FAILED("Payment Failed"),
    PAYMENT_SUCCEEDED("Payment Succeeded"),
    AUTO_RENEWAL_ENABLED("Auto-Renewal Enabled"),
    AUTO_RENEWAL_DISABLED("Auto-Renewal Disabled"),
    PRICE_CHANGED("Price Changed"),
    PROMOTION_APPLIED("Promotion Applied"),
    PROMOTION_REMOVED("Promotion Removed");

    private final String description;

    SubscriptionHistoryChangeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
