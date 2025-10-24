package com.trademaster.subscription.constants;

/**
 * Log Field Constants
 * MANDATORY: Rule #17 - All structured logging field names centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all structured logging field identifiers used in audit logs,
 * business logs, performance logs, and security logs.
 * Ensures consistency across logging, monitoring, and observability.
 *
 * @author TradeMaster Development Team
 */
public final class LogFieldConstants {

    private LogFieldConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Core Context Fields
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String TIMESTAMP = "timestamp";

    // Operation Fields
    public static final String OPERATION = "operation";
    public static final String COMPONENT = "component";
    public static final String SERVICE = "service";
    public static final String SERVICE_OPERATION = "service_operation";
    public static final String ENDPOINT = "endpoint";

    // Status and Severity Fields
    public static final String STATUS = "status";
    public static final String SEVERITY = "severity";
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String ERROR_ID = "errorId";
    public static final String ERROR_TYPE = "error_type";

    // Business Entity Fields
    public static final String SUBSCRIPTION_ID = "subscription_id";
    public static final String TIER = "tier";
    public static final String OLD_TIER = "old_tier";
    public static final String NEW_TIER = "new_tier";
    public static final String BILLING_CYCLE = "billing_cycle";
    public static final String FEATURE = "feature";
    public static final String USAGE_COUNT = "usage_count";
    public static final String USAGE_LIMIT = "usage_limit";

    // Performance Metrics Fields
    public static final String DURATION_MS = "duration_ms";
    public static final String RESPONSE_TIME_MS = "response_time_ms";
    public static final String QUERY_TYPE = "query_type";
    public static final String ROWS_AFFECTED = "rows_affected";

    // Financial Fields
    public static final String AMOUNT = "amount";
    public static final String CURRENCY = "currency";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String PROMO_CODE = "promo_code";

    // Security Fields
    public static final String IP_ADDRESS = "ip_address";
    public static final String USER_AGENT = "user_agent";
    public static final String RESOURCE = "resource";
    public static final String INCIDENT_TYPE = "incident_type";
    public static final String ATTEMPT_COUNT = "attempt_count";
    public static final String AFFECTED_USER_ID = "affectedUserId";

    // Event Fields
    public static final String EVENT_TYPE = "event_type";
    public static final String TRIGGER_DATE = "trigger_date";
    public static final String EXPIRY_DATE = "expiry_date";
    public static final String EFFECTIVE_DATE = "effective_date";

    // Trial Fields
    public static final String TRIAL_END_DATE = "trial_end_date";
    public static final String CONVERTED_TO_TIER = "converted_to_tier";

    // Additional Context Fields
    public static final String CONTEXT = "context";
    public static final String METADATA = "metadata";
    public static final String ADDITIONAL_INFO = "additional_info";
}
