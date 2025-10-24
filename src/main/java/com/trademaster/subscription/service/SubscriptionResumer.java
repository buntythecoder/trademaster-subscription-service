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
 * Subscription Resumer Service
 * MANDATORY: Single Responsibility - Focused on subscription resumption only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SubscriptionResumer extends BaseSubscriptionService {

    private final SubscriptionBusinessLogic businessLogic;

    public SubscriptionResumer(SubscriptionRepository subscriptionRepository,
                              SubscriptionHistoryRepository historyRepository,
                              SubscriptionMetricsService metricsService,
                              StructuredLoggingService loggingService,
                              ApplicationEventPublisher eventPublisher,
                              CircuitBreaker databaseCircuitBreaker,
                              Retry databaseRetry,
                              SubscriptionBusinessLogic businessLogic) {
        super(subscriptionRepository, historyRepository, metricsService,
              loggingService, eventPublisher, databaseCircuitBreaker, databaseRetry);
        this.businessLogic = businessLogic;
    }

    @Transactional
    public CompletableFuture<Result<Subscription, String>> resumeSubscription(UUID subscriptionId) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return findSubscription(subscriptionId)
                .flatMap(this::validateCanResume)
                .flatMap(this::performResumption)
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "resume_subscription");
                    log.info("Subscription resumed: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "resume_failed");
                    log.error("Resumption failed: {}, error: {}", subscriptionId, e);
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

    private Result<Subscription, String> validateCanResume(Subscription s) {
        return switch (s.getStatus()) {
            case SUSPENDED -> Result.success(s);
            case ACTIVE -> Result.failure("Already active");
            case CANCELLED -> Result.failure("Cannot resume cancelled");
            case EXPIRED -> Result.failure("Cannot resume expired");
            case PENDING, TRIAL -> Result.failure("Cannot resume pending/trial");
            default -> Result.failure("Invalid status for resumption");
        };
    }

    private Result<Subscription, String> performResumption(Subscription s) {
        s.setStatus(SubscriptionStatus.ACTIVE);
        businessLogic.updateNextBillingDate(s);
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
                    .action("SUBSCRIPTION_RESUMED").oldTier(s.getTier()).newTier(s.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Resumed after suspension")
                    .initiatedBy(SubscriptionHistoryInitiatedBy.USER).build());
                return s;
            }).mapError(ex -> "Failed to record history: " + ex.getMessage())
        );
    }
}
