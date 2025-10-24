package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.base.BaseBillingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Billing Cycle Manager
 * MANDATORY: Single Responsibility - Handles billing cycle updates only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class BillingCycleManager extends BaseBillingService {

    private final SubscriptionBusinessLogic businessLogic;

    public BillingCycleManager(SubscriptionRepository subscriptionRepository,
                              SubscriptionHistoryRepository historyRepository,
                              SubscriptionMetricsService metricsService,
                              StructuredLoggingService loggingService,
                              ApplicationEventPublisher eventPublisher,
                              CircuitBreaker databaseCircuitBreaker,
                              Retry databaseRetry,
                              SubscriptionBusinessLogic businessLogic) {
        super(subscriptionRepository, historyRepository, metricsService, loggingService,
              eventPublisher, databaseCircuitBreaker, databaseRetry);
        this.businessLogic = businessLogic;
    }

    @Transactional
    public CompletableFuture<Result<Subscription, String>> updateBillingCycle(
            UUID subId, BillingCycle newCycle) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return initContext(subId, newCycle, corrId)
                .flatMap(this::findSubscription)
                .flatMap(this::validateChange)
                .flatMap(this::updateCycle)
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .map(CycleCtx::subscription)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "update_billing_cycle");
                    log.info("Billing cycle updated: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "update_billing_cycle_failed");
                    log.error("Cycle update failed: {}, error: {}", subId, e);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<CycleCtx, String> initContext(UUID subId, BillingCycle newCycle, String corrId) {
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            log.info("Updating billing cycle: sub={}, new={}", subId, newCycle);
            return new CycleCtx(corrId, subId, newCycle, null, null);
        }).mapError(ex -> "Failed to init context: " + ex.getMessage());
    }

    private Result<CycleCtx, String> findSubscription(CycleCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findById(ctx.subscriptionId()))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.map(sub -> Result.<CycleCtx, String>success(
                    new CycleCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.newCycle(),
                                sub, sub.getBillingCycle())))
                    .orElse(Result.failure("Subscription not found: " + ctx.subscriptionId())))
        );
    }

    private Result<CycleCtx, String> validateChange(CycleCtx ctx) {
        return switch (ctx.subscription().getStatus()) {
            case ACTIVE, TRIAL -> validateCycleCompatibility(ctx);
            case PENDING -> Result.failure("Cannot change cycle for pending");
            case CANCELLED, EXPIRED -> Result.failure("Cannot change cycle for cancelled/expired");
            case SUSPENDED -> Result.failure("Cannot change cycle for suspended");
            default -> Result.failure("Invalid status for cycle change");
        };
    }

    private Result<CycleCtx, String> validateCycleCompatibility(CycleCtx ctx) {
        return ctx.oldCycle() == ctx.newCycle()
            ? Result.failure("Already on cycle: " + ctx.newCycle())
            : Result.success(ctx);
    }

    private Result<CycleCtx, String> updateCycle(CycleCtx ctx) {
        return Result.tryExecute(() -> {
            Subscription s = ctx.subscription();
            s.setBillingCycle(ctx.newCycle());
            BigDecimal newAmt = calculateBillingAmount(s.getTier(), ctx.newCycle());
            s.setBillingAmount(newAmt);
            businessLogic.updateNextBillingDate(s);
            return new CycleCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.newCycle(),
                               s, ctx.oldCycle());
        }).mapError(ex -> "Failed to update cycle: " + ex.getMessage());
    }

    private Result<CycleCtx, String> save(CycleCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                subscriptionRepository.save(ctx.subscription());
                return ctx;
            }).mapError(ex -> "Failed to save: " + ex.getMessage())
        );
    }

    private Result<CycleCtx, String> recordHistory(CycleCtx ctx) {
        String reason = "Billing cycle changed from " + ctx.oldCycle() + " to " + ctx.newCycle();
        return saveHistory(ctx.subscription(), "BILLING_CYCLE_CHANGED", reason,
                          SubscriptionHistoryInitiatedBy.USER)
            .map(v -> ctx);
    }

    private record CycleCtx(String correlationId, UUID subscriptionId, BillingCycle newCycle,
                           Subscription subscription, BillingCycle oldCycle) {}
}
