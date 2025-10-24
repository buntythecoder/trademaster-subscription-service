package com.trademaster.subscription.service.base;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.service.SubscriptionMetricsService;
import com.trademaster.subscription.service.StructuredLoggingService;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Base Notification Service
 * MANDATORY: DRY Principle - Shared infrastructure for notification services
 * MANDATORY: Single Responsibility - Provides common notification infrastructure only
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseNotificationService {

    protected final ApplicationEventPublisher eventPublisher;
    protected final SubscriptionMetricsService metricsService;
    protected final StructuredLoggingService loggingService;
    protected final CircuitBreaker notificationCircuitBreaker;
    protected final Retry notificationRetry;

    protected <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return notificationCircuitBreaker.executeSupplier(operation);
    }

    protected Executor getVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
