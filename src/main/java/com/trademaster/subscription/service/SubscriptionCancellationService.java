package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.constants.OperationNameConstants;
import com.trademaster.subscription.constants.SubscriptionEventConstants;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Cancellation Service
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription cancellation and termination only
 * MANDATORY: Functional Programming - Rule #3 (no if-else, Railway pattern)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class SubscriptionCancellationService extends BaseSubscriptionService {

    public SubscriptionCancellationService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionHistoryRepository historyRepository,
            SubscriptionMetricsService metricsService,
            StructuredLoggingService loggingService,
            ApplicationEventPublisher eventPublisher,
            CircuitBreaker databaseCircuitBreaker,
            Retry databaseRetry) {
        super(subscriptionRepository, historyRepository, metricsService,
              loggingService, eventPublisher, databaseCircuitBreaker, databaseRetry);
    }

    /**
     * Cancel subscription
     * MANDATORY: Functional Programming - Railway pattern with CompletableFuture
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> cancelSubscription(
            UUID subscriptionId, String cancellationReason) {

        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return initializeCancellationContext(subscriptionId, cancellationReason, correlationId)
                .flatMap(this::findSubscriptionForCancellation)
                .flatMap(this::validateCanCancel)
                .flatMap(this::performCancellation)
                .flatMap(this::saveCancelledSubscription)
                .flatMap(this::recordCancellationHistory)
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.CANCEL_SUBSCRIPTION);
                    log.info("Subscription cancelled successfully: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.CANCEL_SUBSCRIPTION_FAILED);
                    log.error("Failed to cancel subscription: {}, error: {}", subscriptionId, error);
                });
        }, getVirtualThreadExecutor());
    }

    // Private helper methods - Cancellation flow

    private Result<CancellationContext, String> initializeCancellationContext(
            UUID subscriptionId, String cancellationReason, String correlationId) {
        return Result.success(new CancellationContext(correlationId, subscriptionId, cancellationReason, null));
    }

    private Result<CancellationContext, String> findSubscriptionForCancellation(CancellationContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult =
                Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            return repoResult.mapError(Exception::getMessage)
                .flatMap(subscriptionOpt -> subscriptionOpt
                    .map(subscription -> Result.<CancellationContext, String>success(
                        new CancellationContext(context.correlationId(), context.subscriptionId(),
                                              context.cancellationReason(), subscription)))
                    .orElse(Result.failure("Subscription not found: " + context.subscriptionId())));
        });
    }

    private Result<CancellationContext, String> validateCanCancel(CancellationContext context) {
        return switch (context.subscription().getStatus()) {
            case ACTIVE, TRIAL, SUSPENDED -> Result.success(context);
            case PENDING -> Result.failure("Cannot cancel pending subscription - contact support");
            case CANCELLED -> Result.failure("Subscription is already cancelled");
            case EXPIRED -> Result.failure("Cannot cancel expired subscription");
            default -> Result.failure("Invalid subscription status for cancellation");
        };
    }

    private Result<Subscription, String> performCancellation(CancellationContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            subscription.setCancellationReason(context.cancellationReason());
            subscription.setAutoRenewal(false);
            return subscription;
        }).mapError(exception -> "Failed to perform cancellation: " + exception.getMessage());
    }

    private Result<Subscription, String> saveCancelledSubscription(Subscription subscription) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.save(subscription))
                .mapError(exception -> "Failed to save cancelled subscription: " + exception.getMessage())
        );
    }

    private Result<Subscription, String> recordCancellationHistory(Subscription subscription) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUserId())
                    .action(SubscriptionEventConstants.SUBSCRIPTION_CANCELLED)
                    .oldTier(subscription.getTier())
                    .newTier(subscription.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription cancelled by user request")
                    .initiatedBy(SubscriptionHistoryInitiatedBy.USER)
                    .build();
                historyRepository.save(history);
                return subscription;
            }).mapError(exception -> "Failed to record cancellation history: " + exception.getMessage())
        );
    }

    // Context Record - MANDATORY: Immutability - Rule #9
    private record CancellationContext(
        String correlationId, UUID subscriptionId,
        String cancellationReason, Subscription subscription
    ) {}
}
