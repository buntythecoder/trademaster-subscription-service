package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.LogFieldConstants;
import com.trademaster.subscription.service.base.BaseLoggingService;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Business Audit Logger
 * MANDATORY: Single Responsibility - Handles business event audit logging only
 * MANDATORY: Compliance - All business events logged for regulatory requirements
 *
 * Logs all business-critical events for audit trail and compliance.
 * Includes subscription lifecycle, billing, tier changes, usage, trials, and promotions.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class BusinessAuditLogger extends BaseLoggingService {

    /**
     * Log subscription lifecycle event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logSubscriptionEvent(String operation, String subscriptionId, String userId,
                                    String tier, String status, String billingCycle,
                                    String amount, String currency) {
        BUSINESS_AUDIT.info("Subscription event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("billing_cycle", billingCycle),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log billing event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logBillingEvent(String operation, String subscriptionId, String userId,
                               String tier, String amount, String currency, String status,
                               String paymentMethod, Map<String, Object> metadata) {
        BUSINESS_AUDIT.info("Billing event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("amount", amount),
            StructuredArguments.kv("currency", currency),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("payment_method", paymentMethod),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("metadata", metadata)
        );
    }

    /**
     * Log tier change event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logTierChangeEvent(String operation, String subscriptionId, String userId,
                                  String oldTier, String newTier, String reason,
                                  String oldAmount, String newAmount) {
        BUSINESS_AUDIT.info("Tier change event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("old_tier", oldTier),
            StructuredArguments.kv("new_tier", newTier),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("old_amount", oldAmount),
            StructuredArguments.kv("new_amount", newAmount),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log usage tracking event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logUsageEvent(String operation, String userId, String subscriptionId,
                             String feature, String currentUsage, String usageLimit,
                             String status, Map<String, Object> details) {
        BUSINESS_AUDIT.info("Usage tracking event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv("feature", feature),
            StructuredArguments.kv("current_usage", currentUsage),
            StructuredArguments.kv("usage_limit", usageLimit),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }

    /**
     * Log trial event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logTrialEvent(String operation, String subscriptionId, String userId,
                             String tier, String trialDays, String status) {
        BUSINESS_AUDIT.info("Trial event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("tier", tier),
            StructuredArguments.kv("trial_days", trialDays),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log promotion code event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logPromoCodeEvent(String operation, String subscriptionId, String userId,
                                 String promoCode, String discount, String status) {
        BUSINESS_AUDIT.info("Promotion code event",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("subscription_id", subscriptionId),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("promo_code", promoCode),
            StructuredArguments.kv("discount", discount),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }
}
