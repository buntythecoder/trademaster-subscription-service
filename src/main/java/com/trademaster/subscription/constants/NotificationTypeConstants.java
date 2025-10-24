package com.trademaster.subscription.constants;

/**
 * Notification Type Constants
 * MANDATORY: Rule #17 - Constants & Magic Numbers
 *
 * Centralized notification type definitions for user communication.
 * Used for email, SMS, and in-app notification routing.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class NotificationTypeConstants {

    // Subscription Lifecycle Notifications
    public static final String NOTIFICATION_SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
    public static final String NOTIFICATION_SUBSCRIPTION_ACTIVATED = "SUBSCRIPTION_ACTIVATED";
    public static final String NOTIFICATION_SUBSCRIPTION_CANCELLED = "SUBSCRIPTION_CANCELLED";
    public static final String NOTIFICATION_SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";

    // Tier Change Notifications
    public static final String NOTIFICATION_SUBSCRIPTION_UPGRADED = "SUBSCRIPTION_UPGRADED";
    public static final String NOTIFICATION_SUBSCRIPTION_DOWNGRADED = "SUBSCRIPTION_DOWNGRADED";

    // Billing Notifications
    public static final String NOTIFICATION_PAYMENT_SUCCESSFUL = "PAYMENT_SUCCESSFUL";
    public static final String NOTIFICATION_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String NOTIFICATION_BILLING_REMINDER = "BILLING_REMINDER";

    // Trial Notifications
    public static final String NOTIFICATION_TRIAL_STARTED = "TRIAL_STARTED";
    public static final String NOTIFICATION_TRIAL_ENDING_SOON = "TRIAL_ENDING_SOON";
    public static final String NOTIFICATION_TRIAL_EXPIRED = "TRIAL_EXPIRED";

    // Usage Notifications
    public static final String NOTIFICATION_USAGE_WARNING = "USAGE_WARNING";
    public static final String NOTIFICATION_USAGE_LIMIT_REACHED = "USAGE_LIMIT_REACHED";

    private NotificationTypeConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
