package com.trademaster.subscription.service;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MANDATORY Structured Logging Service
 * 
 * Provides structured JSON logging for ELK stack integration as per TradeMaster standards.
 * Implements context preservation for Virtual Thread operations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class StructuredLoggingService {

    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger BUSINESS_AUDIT = LoggerFactory.getLogger("BUSINESS_AUDIT");
    private static final Logger PERFORMANCE = LoggerFactory.getLogger("PERFORMANCE");
    
    // Context Keys
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String USER_AGENT = "userAgent";
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    private static final String TRANSACTION_ID = "transactionId";
    
    /**
     * Set correlation ID for request tracking
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId != null ? correlationId : UUID.randomUUID().toString());
    }
    
    /**
     * Set user context for logging
     */
    public void setUserContext(String userId, String sessionId, String ipAddress, String userAgent) {
        if (userId != null) MDC.put(USER_ID, userId);
        if (sessionId != null) MDC.put(SESSION_ID, sessionId);
        if (ipAddress != null) MDC.put(IP_ADDRESS, ipAddress);
        if (userAgent != null) MDC.put(USER_AGENT, userAgent);
    }
    
    /**
     * Set business context for logging
     */
    public void setBusinessContext(String subscriptionId, String transactionId) {
        if (subscriptionId != null) MDC.put(SUBSCRIPTION_ID, subscriptionId);
        if (transactionId != null) MDC.put(TRANSACTION_ID, transactionId);
    }
    
    /**
     * Clear all MDC context
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific context key
     */
    public void clearContext(String key) {
        MDC.remove(key);
    }
    
    // Business Audit Logs - MANDATORY for Subscription Compliance
    
    public void logSubscriptionEvent(String operation, String subscriptionId, String userId, 
                                   String tier, String status, String billingCycle, 
                                   String amount, String currency) {
        BUSINESS_AUDIT.info("Subscription event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("billing_cycle", billingCycle),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logBillingEvent(String operation, String subscriptionId, String userId, 
                               String tier, String amount, String currency, String status,
                               String paymentMethod, Map<String, Object> metadata) {
        BUSINESS_AUDIT.info("Billing event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("payment_method", paymentMethod),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("metadata", metadata)
        );
    }
    
    public void logTierChangeEvent(String operation, String subscriptionId, String userId,
                                  String oldTier, String newTier, String reason, 
                                  String oldAmount, String newAmount) {
        BUSINESS_AUDIT.info("Tier change event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("old_tier", oldTier),
            StructuredArguments.kv("new_tier", newTier),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("old_amount", oldAmount),
            StructuredArguments.kv("new_amount", newAmount),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logUsageEvent(String operation, String userId, String subscriptionId,
                             String feature, String currentUsage, String usageLimit,
                             String status, Map<String, Object> details) {
        BUSINESS_AUDIT.info("Usage tracking event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("feature", feature),
            StructuredArguments.kv("current_usage", currentUsage),
            StructuredArguments.kv("usage_limit", usageLimit),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }
    
    public void logTrialEvent(String operation, String subscriptionId, String userId,
                             String tier, String trialDays, String status) {
        BUSINESS_AUDIT.info("Trial event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("trial_days", trialDays),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logPromoCodeEvent(String operation, String subscriptionId, String userId,
                                 String promoCode, String discount, String status) {
        BUSINESS_AUDIT.info("Promotion code event",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("promo_code", promoCode),
            StructuredArguments.kv("discount", discount),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Security Audit Logs - MANDATORY per standards
    
    public void logSecurityIncident(String incidentType, String severity, String userId, 
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        SECURITY_AUDIT.warn("Security incident detected",
            StructuredArguments.kv("incident_type", incidentType),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("operation", "security_incident"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }
    
    public void logRateLimitViolation(String endpoint, String userId, String ipAddress, 
                                    String userAgent, String violationType) {
        SECURITY_AUDIT.warn("Rate limit violation",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("violation_type", violationType),
            StructuredArguments.kv("operation", "rate_limit_violation"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logUnauthorizedAccess(String resource, String userId, String ipAddress, 
                                     String userAgent, String attemptedAction) {
        SECURITY_AUDIT.warn("Unauthorized access attempt",
            StructuredArguments.kv("resource", resource),
            StructuredArguments.kv("user_id", userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("attempted_action", attemptedAction),
            StructuredArguments.kv("operation", "unauthorized_access"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Performance Logs - MANDATORY per standards
    
    public void logPerformanceMetric(String operation, String component, long durationMs, 
                                   String status, Map<String, Object> additionalMetrics) {
        PERFORMANCE.info("Performance metric",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("component", component),
            StructuredArguments.kv("duration_ms", durationMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("metrics", additionalMetrics)
        );
    }
    
    public void logDatabasePerformance(String query, long executionTimeMs, String status, 
                                     int recordsAffected) {
        PERFORMANCE.info("Database performance",
            StructuredArguments.kv("operation", "database_query"),
            StructuredArguments.kv("query_type", query),
            StructuredArguments.kv("execution_time_ms", executionTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("records_affected", recordsAffected),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logServicePerformance(String service, String operation, long responseTimeMs, 
                                    String status, String responseCode) {
        PERFORMANCE.info("Service performance",
            StructuredArguments.kv("operation", "service_call"),
            StructuredArguments.kv("service", service),
            StructuredArguments.kv("service_operation", operation),
            StructuredArguments.kv("response_time_ms", responseTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("response_code", responseCode),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    // Application Logs - Standard structured logging
    
    public void logInfo(String operation, String message, Map<String, Object> context) {
        log.info("{}",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("message", message),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }
    
    public void logError(String operation, String errorMessage, String errorCode, 
                        Throwable throwable, Map<String, Object> context) {
        log.error("Error in operation",
            StructuredArguments.kv("operation", operation),
            StructuredArguments.kv("error_message", errorMessage),
            StructuredArguments.kv("error_code", errorCode),
            StructuredArguments.kv("exception_class", throwable != null ? throwable.getClass().getSimpleName() : null),
            StructuredArguments.kv("stack_trace", throwable != null ? throwable.getMessage() : null),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("context", context),
            throwable
        );
    }
}