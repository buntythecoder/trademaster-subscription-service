package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.MetricNameConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Subscription Metrics Recorder
 * MANDATORY: Single Responsibility - Handles business metrics recording only
 * MANDATORY: Performance Monitoring - All business events tracked for analysis
 *
 * Records business metrics for subscriptions, billing, usage, and analytics.
 * Provides Prometheus counters for business intelligence and operational insights.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionMetricsRecorder {

    private final MeterRegistry meterRegistry;

    /**
     * Record subscription created event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionCreated(String tier) {
        Counter.builder(MetricNameConstants.SUBSCRIPTIONS_CREATED_TOTAL)
            .description("Total number of subscriptions created")
            .tag("tier", tier)
            .register(meterRegistry)
            .increment();
        log.debug("Subscription created metric recorded for tier: {}", tier);
    }

    /**
     * Record subscription activated event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionActivated(String tier, String billingCycle) {
        Counter.builder(MetricNameConstants.SUBSCRIPTIONS_ACTIVATED_TOTAL)
            .description("Total number of subscriptions activated")
            .tag("tier", tier)
            .tag("billing_cycle", billingCycle)
            .register(meterRegistry)
            .increment();
        log.debug("Subscription activated metric recorded for tier: {}, cycle: {}", tier, billingCycle);
    }

    /**
     * Record subscription cancelled event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionCancelled(String tier, String reason) {
        Counter.builder(MetricNameConstants.SUBSCRIPTIONS_CANCELLED)
            .tag("tier", tier)
            .tag("reason", reason != null ? reason.toLowerCase().replaceAll("\\s+", "_") : "unknown")
            .register(meterRegistry)
            .increment();
        log.debug("Subscription cancelled metric recorded for tier: {}, reason: {}", tier, reason);
    }

    /**
     * Record subscription upgraded event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionUpgraded(String fromTier, String toTier) {
        Counter.builder(MetricNameConstants.SUBSCRIPTIONS_UPGRADED)
            .tag("from_tier", fromTier)
            .tag("to_tier", toTier)
            .register(meterRegistry)
            .increment();
        log.debug("Subscription upgraded metric recorded from: {} to: {}", fromTier, toTier);
    }

    /**
     * Record subscription downgraded event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionDowngraded(String fromTier, String toTier) {
        Counter.builder(MetricNameConstants.SUBSCRIPTIONS_DOWNGRADED)
            .tag("from_tier", fromTier)
            .tag("to_tier", toTier)
            .register(meterRegistry)
            .increment();
        log.debug("Subscription downgraded metric recorded from: {} to: {}", fromTier, toTier);
    }

    /**
     * Record billing successful event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordBillingSuccessful(String tier, String billingCycle, long processingTimeMs) {
        Counter.builder(MetricNameConstants.BILLING_SUCCESSFUL)
            .tag("tier", tier)
            .tag("billing_cycle", billingCycle)
            .register(meterRegistry)
            .increment();

        Timer.builder(MetricNameConstants.BILLING_PROCESSING_DURATION)
            .description("Time taken to process billing operations")
            .tag("tier", tier)
            .tag("billing_cycle", billingCycle)
            .register(meterRegistry)
            .record(Duration.ofMillis(processingTimeMs));

        log.debug("Billing successful metric recorded for tier: {}, cycle: {}, time: {}ms",
                 tier, billingCycle, processingTimeMs);
    }

    /**
     * Record billing failed event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordBillingFailed(String tier, String billingCycle, String errorType) {
        Counter.builder(MetricNameConstants.BILLING_FAILED)
            .tag("tier", tier)
            .tag("billing_cycle", billingCycle)
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment();
        log.debug("Billing failed metric recorded for tier: {}, cycle: {}, error: {}",
                 tier, billingCycle, errorType);
    }

    /**
     * Record usage limit exceeded event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordUsageLimitExceeded(String userId, String feature, String tier) {
        Counter.builder(MetricNameConstants.USAGE_LIMIT_EXCEEDED)
            .tag("feature", feature)
            .tag("tier", tier)
            .register(meterRegistry)
            .increment();
        log.debug("Usage limit exceeded metric recorded for user: {}, feature: {}, tier: {}",
                 userId, feature, tier);
    }

    /**
     * Record trial conversion event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordTrialConversion(String tier, boolean converted) {
        Counter.builder("trial_conversions_total")
            .description("Total number of trial conversions")
            .tag("tier", tier)
            .tag("converted", String.valueOf(converted))
            .register(meterRegistry)
            .increment();

        log.debug("Trial conversion recorded for tier: {}, converted: {}", tier, converted);
    }

    /**
     * Record churn event with subscription lifetime
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordChurnEvent(String tier, String churnReason, long subscriptionAgeInDays) {
        Counter.builder("churn_events_total")
            .description("Total number of churn events")
            .tag("tier", tier)
            .tag("reason", churnReason != null ? churnReason.toLowerCase().replaceAll("\\s+", "_") : "unknown")
            .register(meterRegistry)
            .increment();

        Timer.builder("subscription_lifetime_days")
            .description("Subscription lifetime in days at churn")
            .tag("tier", tier)
            .register(meterRegistry)
            .record(Duration.ofDays(subscriptionAgeInDays));

        log.debug("Churn event recorded for tier: {}, reason: {}, age: {} days",
                 tier, churnReason, subscriptionAgeInDays);
    }

    /**
     * Record price change event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordPriceChange(String tier, double oldPrice, double newPrice) {
        Counter.builder("price_changes_total")
            .description("Total number of price changes")
            .tag("tier", tier)
            .tag("change_type", newPrice > oldPrice ? "increase" : "decrease")
            .register(meterRegistry)
            .increment();

        log.debug("Price change recorded for tier: {}, old: {}, new: {}", tier, oldPrice, newPrice);
    }
}
