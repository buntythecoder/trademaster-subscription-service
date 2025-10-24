package com.trademaster.subscription.constants;

/**
 * Operation Name Constants
 * MANDATORY: Rule #17 - All operation identifiers centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all operation identifiers used in metrics, logging, and tracing.
 * Ensures consistency across performance monitoring and observability.
 *
 * @author TradeMaster Development Team
 */
public final class OperationNameConstants {

    private OperationNameConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Subscription Operations
    public static final String CREATE_SUBSCRIPTION = "create_subscription";
    public static final String CREATE_FAILED = "create_failed";
    public static final String ACTIVATE_SUBSCRIPTION = "activate_subscription";
    public static final String ACTIVATE_FAILED = "activate_failed";
    public static final String CANCEL_SUBSCRIPTION = "cancel_subscription";
    public static final String CANCEL_SUBSCRIPTION_FAILED = "cancel_subscription_failed";
    public static final String UPGRADE_SUBSCRIPTION = "upgrade_subscription";
    public static final String UPGRADE_SUBSCRIPTION_FAILED = "upgrade_subscription_failed";
    public static final String SUSPEND_SUBSCRIPTION = "suspend_subscription";
    public static final String SUSPEND_FAILED = "suspend_failed";
    public static final String RESUME_SUBSCRIPTION = "resume_subscription";
    public static final String RESUME_FAILED = "resume_failed";

    // Billing Operations
    public static final String PROCESS_BILLING = "process_billing";
    public static final String PROCESS_BILLING_FAILED = "process_billing_failed";
    public static final String UPDATE_BILLING_CYCLE = "update_billing_cycle";
    public static final String UPDATE_BILLING_CYCLE_FAILED = "update_billing_cycle_failed";

    // Usage Operations
    public static final String CAN_USE_FEATURE = "can_use_feature";
    public static final String CAN_USE_FEATURE_FAILED = "can_use_feature_failed";
    public static final String INCREMENT_USAGE = "increment_usage";
    public static final String INCREMENT_USAGE_FAILED = "increment_usage_failed";
    public static final String RESET_USAGE = "reset_usage";
    public static final String RESET_USAGE_FAILED = "reset_usage_failed";

    // Notification Operations
    public static final String PUBLISH_NOTIFICATION = "publish_notification";
    public static final String PUBLISH_NOTIFICATION_FAILED = "publish_notification_failed";
    public static final String BATCH_PROCESS_NOTIFICATIONS = "batch_process_notifications";
    public static final String BATCH_PROCESS_FAILED = "batch_process_failed";

    // Database Operations
    public static final String DATABASE_QUERY = "database_query";
    public static final String SERVICE_CALL = "service_call";

    // Security Operations
    public static final String SECURITY_INCIDENT = "security_incident";
    public static final String RATE_LIMIT_VIOLATION = "rate_limit_violation";
    public static final String UNAUTHORIZED_ACCESS = "unauthorized_access";
}
