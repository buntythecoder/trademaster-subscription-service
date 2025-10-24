package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.MetricNameConstants;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance Timer Service
 * MANDATORY: Single Responsibility - Handles performance timers and gauge management only
 * MANDATORY: Performance Monitoring - All timing and gauge metrics tracked
 *
 * Manages performance timing metrics and gauge-based metrics for subscription state.
 * Provides timer samples for operation timing and gauge updates for system state.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PerformanceTimerService {

    private final MeterRegistry meterRegistry;

    // Gauges for current state
    private final AtomicLong activeSubscriptions = new AtomicLong(0);
    private final AtomicLong trialSubscriptions = new AtomicLong(0);
    private final AtomicLong suspendedSubscriptions = new AtomicLong(0);
    private final AtomicLong currentMRR = new AtomicLong(0);
    private final AtomicLong currentARR = new AtomicLong(0);

    /**
     * Initialize gauge metrics
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    @PostConstruct
    public void initializeMetrics() {
        Gauge.builder(MetricNameConstants.ACTIVE_SUBSCRIPTIONS, activeSubscriptions, AtomicLong::doubleValue)
            .description("Current number of active subscriptions")
            .register(meterRegistry);

        Gauge.builder(MetricNameConstants.TRIAL_SUBSCRIPTIONS, trialSubscriptions, AtomicLong::doubleValue)
            .description("Current number of trial subscriptions")
            .register(meterRegistry);

        Gauge.builder(MetricNameConstants.SUSPENDED_SUBSCRIPTIONS, suspendedSubscriptions, AtomicLong::doubleValue)
            .description("Current number of suspended subscriptions")
            .register(meterRegistry);

        Gauge.builder(MetricNameConstants.MONTHLY_RECURRING_REVENUE, currentMRR, AtomicLong::doubleValue)
            .description("Current monthly recurring revenue in INR")
            .register(meterRegistry);

        Gauge.builder(MetricNameConstants.ANNUAL_RECURRING_REVENUE, currentARR, AtomicLong::doubleValue)
            .description("Current annual recurring revenue in INR")
            .register(meterRegistry);
    }

    // Performance Timer Methods

    /**
     * Start subscription processing timer
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public Timer.Sample startSubscriptionProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record subscription processing time
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordSubscriptionProcessingTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder(MetricNameConstants.SUBSCRIPTION_PROCESSING_DURATION)
            .description("Time taken to process subscription operations")
            .tag(MetricNameConstants.TAG_OPERATION, operation)
            .register(meterRegistry));
    }

    /**
     * Start billing processing timer
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public Timer.Sample startBillingProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record billing processing time
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordBillingProcessingTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder(MetricNameConstants.BILLING_PROCESSING_DURATION)
            .description("Time taken to process billing operations")
            .tag(MetricNameConstants.TAG_OPERATION, operation)
            .register(meterRegistry));
    }

    /**
     * Start usage check timer
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public Timer.Sample startUsageCheckTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record usage check time
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void recordUsageCheckTime(Timer.Sample sample, String feature) {
        sample.stop(Timer.builder(MetricNameConstants.USAGE_CHECK_DURATION)
            .description("Time taken to check usage limits")
            .tag(MetricNameConstants.TAG_FEATURE, feature)
            .register(meterRegistry));
    }

    /**
     * Start notification processing timer
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public Timer.Sample startNotificationProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    // Gauge Update Methods

    /**
     * Update active subscriptions gauge
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void updateActiveSubscriptions(long count) {
        activeSubscriptions.set(count);
        log.debug("Updated active subscriptions gauge to: {}", count);
    }

    /**
     * Update trial subscriptions gauge
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void updateTrialSubscriptions(long count) {
        trialSubscriptions.set(count);
        log.debug("Updated trial subscriptions gauge to: {}", count);
    }

    /**
     * Update suspended subscriptions gauge
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void updateSuspendedSubscriptions(long count) {
        suspendedSubscriptions.set(count);
        log.debug("Updated suspended subscriptions gauge to: {}", count);
    }

    /**
     * Update monthly recurring revenue gauge
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void updateMonthlyRecurringRevenue(long mrr) {
        currentMRR.set(mrr);
        log.debug("Updated MRR gauge to: {}", mrr);
    }

    /**
     * Update annual recurring revenue gauge
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void updateAnnualRecurringRevenue(long arr) {
        currentARR.set(arr);
        log.debug("Updated ARR gauge to: {}", arr);
    }
}
