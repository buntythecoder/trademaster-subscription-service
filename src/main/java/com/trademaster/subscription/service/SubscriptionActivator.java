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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Activator Service
 * MANDATORY: Single Responsibility - Focused on subscription activation only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SubscriptionActivator extends BaseSubscriptionService {

    private final SubscriptionBusinessLogic businessLogic;

    public SubscriptionActivator(SubscriptionRepository subscriptionRepository,
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
    public CompletableFuture<Result<Subscription, String>> activateSubscription(
            UUID subscriptionId, UUID paymentTxId) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return findSubscription(subscriptionId)
                .flatMap(this::validateCanActivate)
                .flatMap(this::performActivation)
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.ACTIVATE_SUBSCRIPTION);
                    log.info("Subscription activated: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.ACTIVATE_FAILED);
                    log.error("Activation failed: {}, error: {}", subscriptionId, e);
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

    private Result<Subscription, String> validateCanActivate(Subscription s) {
        return switch (s.getStatus()) {
            case PENDING, TRIAL -> Result.success(s);
            case ACTIVE -> Result.failure("Already active");
            case CANCELLED, EXPIRED -> Result.failure("Cannot activate cancelled/expired");
            case SUSPENDED -> Result.failure("Cannot activate suspended");
            default -> Result.failure("Invalid status for activation");
        };
    }

    private Result<Subscription, String> performActivation(Subscription s) {
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setActivatedDate(LocalDateTime.now());
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
                    .action(SubscriptionEventConstants.SUBSCRIPTION_ACTIVATED).oldTier(s.getTier()).newTier(s.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Activated after payment")
                    .initiatedBy(SubscriptionHistoryInitiatedBy.SYSTEM).build());
                return s;
            }).mapError(ex -> "Failed to record history: " + ex.getMessage())
        );
    }
}
