package com.trademaster.subscription.service.base;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionMetricsService;
import com.trademaster.subscription.service.StructuredLoggingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Base Billing Service
 * MANDATORY: DRY Principle - Shared infrastructure for billing services
 * MANDATORY: Single Responsibility - Provides common billing infrastructure only
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseBillingService {

    protected final SubscriptionRepository subscriptionRepository;
    protected final SubscriptionHistoryRepository historyRepository;
    protected final SubscriptionMetricsService metricsService;
    protected final StructuredLoggingService loggingService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final CircuitBreaker databaseCircuitBreaker;
    protected final Retry databaseRetry;

    protected <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }

    protected Executor getVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    protected BigDecimal calculateBillingAmount(SubscriptionTier tier, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUAL -> tier.getAnnualPrice();
        };
    }

    protected Result<Void, String> saveHistory(Subscription sub, String action, String reason,
                                              SubscriptionHistoryInitiatedBy initiatedBy) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                historyRepository.save(SubscriptionHistory.builder()
                    .subscriptionId(sub.getId()).userId(sub.getUserId())
                    .action(action).oldTier(sub.getTier()).newTier(sub.getTier())
                    .effectiveDate(LocalDateTime.now()).changeReason(reason)
                    .initiatedBy(initiatedBy).build());
                return (Void) null;
            }).mapError(ex -> "Failed to save history: " + ex.getMessage())
        );
    }
}
