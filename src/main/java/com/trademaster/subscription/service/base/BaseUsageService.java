package com.trademaster.subscription.service.base;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.constants.FeatureNameConstants;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import com.trademaster.subscription.service.SubscriptionMetricsService;
import com.trademaster.subscription.service.StructuredLoggingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Base Usage Service
 * MANDATORY: DRY Principle - Shared infrastructure for usage services
 * MANDATORY: Single Responsibility - Provides common usage infrastructure only
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseUsageService {

    protected final SubscriptionRepository subscriptionRepository;
    protected final UsageTrackingRepository usageTrackingRepository;
    protected final SubscriptionMetricsService metricsService;
    protected final StructuredLoggingService loggingService;
    protected final CircuitBreaker databaseCircuitBreaker;
    protected final Retry databaseRetry;

    protected <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }

    protected Executor getVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    protected boolean hasExceededLimit(UsageTracking usage) {
        return switch (Long.compare(usage.getUsageCount(), usage.getUsageLimit())) {
            case 1 -> true;  // usage > limit
            case 0, -1 -> false;  // usage <= limit
            default -> false;
        };
    }

    protected boolean wouldExceedLimit(UsageTracking usage, int increment) {
        long newUsage = usage.getUsageCount() + increment;
        return switch (Long.compare(newUsage, usage.getUsageLimit())) {
            case 1 -> true;  // would exceed
            case 0, -1 -> false;  // would not exceed
            default -> false;
        };
    }

    protected UsageTracking createDefaultUsage(UUID subId, String feature, SubscriptionTier tier) {
        return UsageTracking.builder()
            .subscriptionId(subId).featureName(feature).usageCount(0L)
            .usageLimit(getFeatureLimit(feature, tier))
            .periodStart(LocalDateTime.now().withDayOfMonth(1))
            .periodEnd(LocalDateTime.now().withDayOfMonth(1).plusMonths(1))
            .resetDate(LocalDateTime.now()).build();
    }

    protected Long getFeatureLimit(String feature, SubscriptionTier tier) {
        return switch (feature) {
            case "FeatureNameConstants.API_CALLS" -> switch (tier) {
                case FREE -> 1000L;
                case PRO -> 10000L;
                case AI_PREMIUM -> 50000L;
                case INSTITUTIONAL -> -1L; // Unlimited
            };
            case "FeatureNameConstants.PORTFOLIOS" -> switch (tier) {
                case FREE -> 3L;
                case PRO -> 10L;
                case AI_PREMIUM -> 50L;
                case INSTITUTIONAL -> -1L;
            };
            case "FeatureNameConstants.WATCHLISTS" -> switch (tier) {
                case FREE -> 5L;
                case PRO -> 25L;
                case AI_PREMIUM -> 100L;
                case INSTITUTIONAL -> -1L;
            };
            case "FeatureNameConstants.ALERTS" -> switch (tier) {
                case FREE -> 10L;
                case PRO -> 100L;
                case AI_PREMIUM -> 500L;
                case INSTITUTIONAL -> -1L;
            };
            case "FeatureNameConstants.AI_INSIGHTS" -> switch (tier) {
                case FREE -> 0L;
                case PRO -> 0L;
                case AI_PREMIUM -> 1000L;
                case INSTITUTIONAL -> -1L;
            };
            default -> 0L;
        };
    }
}
