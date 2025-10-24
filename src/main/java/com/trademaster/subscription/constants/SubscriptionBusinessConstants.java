package com.trademaster.subscription.constants;

import java.math.BigDecimal;

/**
 * Subscription Business Constants
 * MANDATORY: Rule #17 - Constants & Magic Numbers
 *
 * Centralized business rule constants for subscription operations.
 * Contains trial periods, grace periods, discounts, and other business values.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class SubscriptionBusinessConstants {

    // Currency
    public static final String DEFAULT_CURRENCY = "USD";

    // Trial Periods (in days)
    public static final int TRIAL_PERIOD_DAYS = 14;
    public static final int TRIAL_WARNING_DAYS_BEFORE_END = 3;

    // Grace Periods (in days)
    public static final int PAYMENT_GRACE_PERIOD_DAYS = 7;
    public static final int CANCELLATION_GRACE_PERIOD_DAYS = 30;

    // Discounts
    public static final BigDecimal QUARTERLY_DISCOUNT_PERCENT = new BigDecimal("0.10"); // 10% off quarterly
    public static final BigDecimal ANNUAL_DISCOUNT_PERCENT = new BigDecimal("0.20"); // 20% off annual

    // Billing Cycles
    public static final String BILLING_CYCLE_MONTHLY = "MONTHLY";
    public static final String BILLING_CYCLE_QUARTERLY = "QUARTERLY";
    public static final String BILLING_CYCLE_ANNUAL = "ANNUAL";

    // Retry Configuration
    public static final int MAX_PAYMENT_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_HOURS = 24;

    private SubscriptionBusinessConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
