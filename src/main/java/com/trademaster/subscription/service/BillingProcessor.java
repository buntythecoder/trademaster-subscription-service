package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Billing Processor
 * MANDATORY: Single Responsibility - Handles billing processing only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class BillingProcessor extends BaseBillingService {

    private final SubscriptionBusinessLogic businessLogic;

    public BillingProcessor(SubscriptionRepository subscriptionRepository,
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
    public CompletableFuture<Result<Subscription, String>> processBilling(
            UUID subId, UUID txId) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return initContext(subId, txId, corrId)
                .flatMap(this::findSubscription)
                .flatMap(this::validateBilling)
                .flatMap(this::calculateAmount)
                .flatMap(this::processPayment)
                .flatMap(this::updateBillingDate)
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .map(BillingCtx::subscription)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "process_billing");
                    log.info("Billing processed: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "process_billing_failed");
                    log.error("Billing failed: {}, error: {}", subId, e);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<BillingCtx, String> initContext(UUID subId, UUID txId, String corrId) {
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            log.info("Processing billing: sub={}, tx={}", subId, txId);
            return new BillingCtx(corrId, subId, txId, null, null, null);
        }).mapError(ex -> "Failed to init context: " + ex.getMessage());
    }

    private Result<BillingCtx, String> findSubscription(BillingCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findById(ctx.subscriptionId()))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.map(sub -> Result.<BillingCtx, String>success(
                    new BillingCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.txId(),
                                  sub, null, null)))
                    .orElse(Result.failure("Subscription not found: " + ctx.subscriptionId())))
        );
    }

    private Result<BillingCtx, String> validateBilling(BillingCtx ctx) {
        return switch (ctx.subscription().getStatus()) {
            case ACTIVE -> Result.success(ctx);
            case TRIAL -> Result.failure("Cannot bill trial");
            case PENDING -> Result.failure("Cannot bill pending");
            case CANCELLED, EXPIRED -> Result.failure("Cannot bill cancelled/expired");
            case SUSPENDED -> Result.failure("Cannot bill suspended");
            default -> Result.failure("Invalid status: " + ctx.subscription().getStatus());
        };
    }

    private Result<BillingCtx, String> calculateAmount(BillingCtx ctx) {
        return Result.tryExecute(() -> {
            Subscription s = ctx.subscription();
            BigDecimal amt = calculateBillingAmount(s.getTier(), s.getBillingCycle());
            return new BillingCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.txId(),
                                 s, amt, null);
        }).mapError(ex -> "Failed to calculate amount: " + ex.getMessage());
    }

    private Result<BillingCtx, String> processPayment(BillingCtx ctx) {
        return Result.tryExecute(() -> {
            LocalDateTime billingDate = LocalDateTime.now();
            return new BillingCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.txId(),
                                 ctx.subscription(), ctx.amount(), billingDate);
        }).mapError(ex -> "Failed to process payment: " + ex.getMessage());
    }

    private Result<BillingCtx, String> updateBillingDate(BillingCtx ctx) {
        return Result.tryExecute(() -> {
            Subscription s = ctx.subscription();
            s.setLastBilledDate(ctx.billingDate());
            businessLogic.updateNextBillingDate(s);
            return new BillingCtx(ctx.correlationId(), ctx.subscriptionId(), ctx.txId(),
                                 s, ctx.amount(), ctx.billingDate());
        }).mapError(ex -> "Failed to update date: " + ex.getMessage());
    }

    private Result<BillingCtx, String> save(BillingCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                subscriptionRepository.save(ctx.subscription());
                return ctx;
            }).mapError(ex -> "Failed to save: " + ex.getMessage())
        );
    }

    private Result<BillingCtx, String> recordHistory(BillingCtx ctx) {
        return saveHistory(ctx.subscription(), "SUBSCRIPTION_BILLED",
                          "Subscription billed successfully",
                          SubscriptionHistoryInitiatedBy.SYSTEM)
            .map(v -> ctx);
    }

    private record BillingCtx(String correlationId, UUID subscriptionId, UUID txId,
                             Subscription subscription, BigDecimal amount,
                             LocalDateTime billingDate) {}
}
