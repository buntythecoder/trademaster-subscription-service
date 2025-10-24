package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.constants.OperationNameConstants;
import com.trademaster.subscription.constants.SubscriptionEventConstants;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
import com.trademaster.subscription.enums.SubscriptionTier;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Creator Service
 * MANDATORY: Single Responsibility - Focused on subscription creation only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SubscriptionCreator extends BaseSubscriptionService {

    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(
        SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL, SubscriptionStatus.EXPIRED
    );

    private final SubscriptionBusinessLogic businessLogic;

    public SubscriptionCreator(SubscriptionRepository subscriptionRepository,
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
    public CompletableFuture<Result<Subscription, String>> createSubscription(
            UUID userId, SubscriptionTier tier, BillingCycle cycle, boolean startTrial) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            loggingService.setCorrelationId(corrId);
            loggingService.setUserContext(userId.toString(), null, null, null);
            log.info("Creating subscription: user={}, tier={}, cycle={}", userId, tier, cycle);

            return validateNoActiveSubscription(userId)
                .flatMap(u -> createEntity(u, tier, cycle, startTrial))
                .flatMap(this::save)
                .flatMap(this::recordHistory)
                .onSuccess(s -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.CREATE_SUBSCRIPTION);
                    log.info("Subscription created: {}", s.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, OperationNameConstants.CREATE_FAILED);
                    log.error("Creation failed for user: {}, error: {}", userId, e);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<UUID, String> validateNoActiveSubscription(UUID userId) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findActiveByUserId(userId, ACTIVE_STATUSES))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.isEmpty() ? Result.success(userId)
                    : Result.failure("User already has active subscription"))
        );
    }

    private Result<Subscription, String> createEntity(UUID userId, SubscriptionTier tier,
                                                     BillingCycle cycle, boolean trial) {
        return Result.tryExecute(() -> {
            Subscription s = Subscription.builder()
                .userId(userId).tier(tier)
                .status(trial ? SubscriptionStatus.TRIAL : SubscriptionStatus.PENDING)
                .billingCycle(cycle).monthlyPrice(tier.getMonthlyPrice())
                .billingAmount(calculateAmount(tier, cycle))
                .currency("INR").startDate(LocalDateTime.now()).autoRenewal(true).build();

            Optional.of(trial).filter(t -> t)
                .ifPresent(t -> s.setTrialEndDate(LocalDateTime.now().plusDays(7)));

            businessLogic.updateNextBillingDate(s);
            return s;
        }).mapError(ex -> "Failed to create entity: " + ex.getMessage());
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
                    .action(SubscriptionEventConstants.SUBSCRIPTION_CREATED).oldTier(null).newTier(s.getTier())
                    .effectiveDate(LocalDateTime.now()).changeReason("Initial creation")
                    .initiatedBy(SubscriptionHistoryInitiatedBy.SYSTEM).build());
                return s;
            }).mapError(ex -> "Failed to record history: " + ex.getMessage())
        );
    }

    private BigDecimal calculateAmount(SubscriptionTier tier, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUAL -> tier.getAnnualPrice();
        };
    }
}
