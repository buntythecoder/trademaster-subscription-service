package com.trademaster.subscription.service;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Subscription Metrics Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized metrics services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized metrics services:
 * - SubscriptionMetricsRecorder: Business metrics recording
 * - PerformanceTimerService: Performance timers and gauge management
 *
 * Provides Prometheus metrics for subscription management as per TradeMaster standards.
 * Tracks business metrics, performance metrics, and operational health.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionMetricsService {

    private final SubscriptionMetricsRecorder metricsRecorder;
    private final PerformanceTimerService timerService;

    // Business Metrics Recording - Delegates to SubscriptionMetricsRecorder

    public void recordSubscriptionCreated(String tier) {
        metricsRecorder.recordSubscriptionCreated(tier);
    }

    public void recordSubscriptionActivated(String tier, String billingCycle) {
        metricsRecorder.recordSubscriptionActivated(tier, billingCycle);
    }

    public void recordSubscriptionCancelled(String tier, String reason) {
        metricsRecorder.recordSubscriptionCancelled(tier, reason);
    }

    public void recordSubscriptionUpgraded(String fromTier, String toTier) {
        metricsRecorder.recordSubscriptionUpgraded(fromTier, toTier);
    }

    public void recordSubscriptionDowngraded(String fromTier, String toTier) {
        metricsRecorder.recordSubscriptionDowngraded(fromTier, toTier);
    }

    public void recordBillingSuccessful(String tier, String billingCycle, long processingTimeMs) {
        metricsRecorder.recordBillingSuccessful(tier, billingCycle, processingTimeMs);
    }

    public void recordBillingFailed(String tier, String billingCycle, String errorType) {
        metricsRecorder.recordBillingFailed(tier, billingCycle, errorType);
    }

    public void recordUsageLimitExceeded(String userId, String feature, String tier) {
        metricsRecorder.recordUsageLimitExceeded(userId, feature, tier);
    }

    public void recordTrialConversion(String tier, boolean converted) {
        metricsRecorder.recordTrialConversion(tier, converted);
    }

    public void recordChurnEvent(String tier, String churnReason, long subscriptionAgeInDays) {
        metricsRecorder.recordChurnEvent(tier, churnReason, subscriptionAgeInDays);
    }

    public void recordPriceChange(String tier, double oldPrice, double newPrice) {
        metricsRecorder.recordPriceChange(tier, oldPrice, newPrice);
    }

    // Performance Timers - Delegates to PerformanceTimerService

    public Timer.Sample startSubscriptionProcessingTimer() {
        return timerService.startSubscriptionProcessingTimer();
    }

    public void recordSubscriptionProcessingTime(Timer.Sample sample, String operation) {
        timerService.recordSubscriptionProcessingTime(sample, operation);
    }

    public Timer.Sample startBillingProcessingTimer() {
        return timerService.startBillingProcessingTimer();
    }

    public void recordBillingProcessingTime(Timer.Sample sample, String operation) {
        timerService.recordBillingProcessingTime(sample, operation);
    }

    public Timer.Sample startUsageCheckTimer() {
        return timerService.startUsageCheckTimer();
    }

    public void recordUsageCheckTime(Timer.Sample sample, String feature) {
        timerService.recordUsageCheckTime(sample, feature);
    }

    public Timer.Sample startNotificationProcessingTimer() {
        return timerService.startNotificationProcessingTimer();
    }

    // Gauge Updates - Delegates to PerformanceTimerService

    public void updateActiveSubscriptions(long count) {
        timerService.updateActiveSubscriptions(count);
    }

    public void updateTrialSubscriptions(long count) {
        timerService.updateTrialSubscriptions(count);
    }

    public void updateSuspendedSubscriptions(long count) {
        timerService.updateSuspendedSubscriptions(count);
    }

    public void updateMonthlyRecurringRevenue(long mrr) {
        timerService.updateMonthlyRecurringRevenue(mrr);
    }

    public void updateAnnualRecurringRevenue(long arr) {
        timerService.updateAnnualRecurringRevenue(arr);
    }
}
