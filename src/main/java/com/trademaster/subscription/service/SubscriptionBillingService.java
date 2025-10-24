package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.interfaces.ISubscriptionBillingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Billing Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized services:
 * - BillingProcessor: Billing processing operations
 * - BillingCycleManager: Billing cycle management
 *
 * @author TradeMaster Development Team
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingService implements ISubscriptionBillingService {

    private final BillingProcessor billingProcessor;
    private final BillingCycleManager cycleManager;
    private final SubscriptionRepository subscriptionRepository;
    private final CircuitBreaker databaseCircuitBreaker;

    /**
     * Process billing - Delegates to BillingProcessor
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> processBilling(
            UUID subscriptionId, UUID paymentTransactionId) {
        return billingProcessor.processBilling(subscriptionId, paymentTransactionId);
    }

    /**
     * Update billing cycle - Delegates to BillingCycleManager
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> updateBillingCycle(
            UUID subscriptionId, BillingCycle newBillingCycle) {
        return cycleManager.updateBillingCycle(subscriptionId, newBillingCycle);
    }

    /**
     * Get upcoming billing amount - Query operation
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<BigDecimal, String>> getUpcomingBillingAmount(UUID subId) {
        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> subscriptionRepository.findById(subId))
                    .mapError(Exception::getMessage)
                    .flatMap(opt -> opt.map(sub -> Result.<BigDecimal, String>success(
                        calculateAmount(sub.getTier(), sub.getBillingCycle())))
                        .orElse(Result.failure("Subscription not found: " + subId)))),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Get subscriptions due for billing - Query operation
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<Subscription>, String>> getSubscriptionsDueForBilling() {
        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> subscriptionRepository.findSubscriptionsDueForBilling(
                    LocalDateTime.now()))
                    .mapError(Exception::getMessage)),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }

    private BigDecimal calculateAmount(SubscriptionTier tier, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUAL -> tier.getAnnualPrice();
        };
    }
}
