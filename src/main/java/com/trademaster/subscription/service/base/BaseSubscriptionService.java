package com.trademaster.subscription.service.base;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionMetricsService;
import com.trademaster.subscription.service.StructuredLoggingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Base Subscription Service
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Shared infrastructure for all subscription services
 * MANDATORY: DRY principle - centralized common operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseSubscriptionService {

    protected final SubscriptionRepository subscriptionRepository;
    protected final SubscriptionHistoryRepository historyRepository;
    protected final SubscriptionMetricsService metricsService;
    protected final StructuredLoggingService loggingService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final CircuitBreaker databaseCircuitBreaker;
    protected final Retry databaseRetry;

    /**
     * Execute operation with circuit breaker resilience
     * MANDATORY: Functional Programming - Rule #3
     */
    protected <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }

    /**
     * Get virtual thread executor for async operations
     * MANDATORY: Java 24 + Virtual Threads - Rule #1
     */
    protected java.util.concurrent.Executor getVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
