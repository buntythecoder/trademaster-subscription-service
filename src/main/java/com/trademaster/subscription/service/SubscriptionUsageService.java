package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import com.trademaster.subscription.service.interfaces.ISubscriptionUsageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Usage Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized services:
 * - UsageTracker: All usage tracking operations
 *
 * @author TradeMaster Development Team
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUsageService implements ISubscriptionUsageService {

    private final UsageTracker usageTracker;
    private final UsageTrackingRepository usageTrackingRepository;
    private final CircuitBreaker databaseCircuitBreaker;

    /**
     * Check feature usage - Delegates to UsageTracker
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Boolean, String>> canUseFeature(
            UUID subscriptionId, String featureName) {
        return usageTracker.canUseFeature(subscriptionId, featureName);
    }

    /**
     * Increment usage - Delegates to UsageTracker
     */
    @Override
    @Transactional
    public CompletableFuture<Result<UsageTracking, String>> incrementUsage(
            UUID subscriptionId, String featureName, int incrementBy) {
        return usageTracker.incrementUsage(subscriptionId, featureName, incrementBy);
    }

    /**
     * Get current usage - Query operation
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<UsageTracking>, String>> getCurrentUsage(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> usageTrackingRepository.findBySubscriptionId(subscriptionId))
                    .mapError(Exception::getMessage)),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Reset usage - Delegates to UsageTracker
     */
    @Override
    @Transactional
    public CompletableFuture<Result<List<UsageTracking>, String>> resetUsageForBillingPeriod(
            UUID subscriptionId) {
        return usageTracker.resetUsage(subscriptionId);
    }

    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
}
