package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.base.BaseSubscriptionService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Suspender Service
 * MANDATORY: Single Responsibility - Focused on subscription suspension only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SubscriptionSuspender extends BaseSubscriptionService {

    public SubscriptionSuspender(SubscriptionRepository subscriptionRepository,
                                SubscriptionHistoryRepository historyRepository,
                                SubscriptionMetricsService metricsService,
                                StructuredLoggingService loggingService,
                                ApplicationEventPublisher eventPublisher,
                                CircuitBreaker databaseCircuitBreaker,
                                Retry databaseRetry) {
        super(subscriptionRepository, historyRepository, metricsService,
              loggingService, eventPublisher, databaseCircuitBreaker, databaseRetry);
    }

    @Transactional
    public CompletableFuture<Result<Subscription, String>> suspendSubscription(
            UUID subscriptionId, String reason) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return findSubscription(subscriptionId)
                .flatMap(this::validateCanSuspend)
                .flatMap(this::performSuspension)
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "suspend_subscription");
                    log.info("Subscription suspended: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "suspend_failed");
                    log.error("Suspension failed: {}, error: {}", subscriptionId, e);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<Subscription, String> findSubscription(UUID id) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findById(id))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.map(Result::<Subscription, String>success)
                    .orElse(Result.failure("Subscription not found: " + id)))
        );
    }

    private Result<Subscription, String> validateCanSuspend(Subscription s) {
        return switch (s.getStatus()) {
            case ACTIVE, TRIAL, EXPIRED -> Result.success(s);
            case SUSPENDED -> Result.failure("Already suspended");
            case CANCELLED -> Result.failure("Cannot suspend cancelled");
            case PENDING -> Result.failure("Cannot suspend pending");
            default -> Result.failure("Invalid status for suspension");
        };
    }

    private Result<Subscription, String> performSuspension(Subscription s) {
        s.setStatus(SubscriptionStatus.SUSPENDED);
        return Result.success(s);
    }

    private Result<Subscription, String> save(Subscription s) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.save(s))
                .mapError(ex -> "Failed to save: " + ex.getMessage())
        );
    }

    private Result<Subscription, String> recordHistory(Subscription s) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                historyRepository.save(SubscriptionHistory.builder()
                    .subscriptionId(s.getId()).userId(s.getUserId())
                    .action("SUBSCRIPTION_SUSPENDED").oldTier(s.getTier()).newTier(s.getTier())
                    .effectiveDate(LocalDateTime.now()).changeReason("Subscription suspended")
                    .initiatedBy(SubscriptionHistoryInitiatedBy.SYSTEM).build());
                return s;
            }).mapError(ex -> "Failed to record history: " + ex.getMessage())
        );
    }
}
