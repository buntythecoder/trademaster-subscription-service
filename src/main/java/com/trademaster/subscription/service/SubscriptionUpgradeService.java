package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.service.interfaces.ISubscriptionUpgradeService;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Upgrade Service
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription tier upgrade operations only
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUpgradeService implements ISubscriptionUpgradeService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final SubscriptionMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    
    // Circuit Breakers for Resilience
    private final CircuitBreaker databaseCircuitBreaker;
    private final Retry databaseRetry;

    /**
     * Upgrade subscription tier
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> upgradeSubscription(
            UUID subscriptionId, SubscriptionTier newTier) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeUpgradeContext(subscriptionId, newTier, correlationId)
                .flatMap(this::findSubscriptionWithResilience)
                .flatMap(this::validateCanUpgrade)
                .flatMap(this::applyTierUpgrade)
                .flatMap(context -> saveUpgradedSubscription(context)
                    .flatMap(subscription -> updateUsageLimitsForNewTier(subscription))
                    .flatMap(updatedSubscription -> recordUpgradeHistory(context)))                        
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "upgrade_subscription");
                    logSuccessfulUpgrade(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "upgrade_subscription_failed");
                    logUpgradeFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    // Private helper methods with functional programming patterns
    
    private Result<UpgradeContext, String> initializeUpgradeContext(
            UUID subscriptionId, SubscriptionTier newTier, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.info("Upgrading subscription: {} to tier: {}", subscriptionId, newTier);
            
            return new UpgradeContext(correlationId, subscriptionId, newTier, null, null);
        }).mapError(exception -> "Failed to initialize upgrade context: " + exception.getMessage());
    }
    
    private Result<UpgradeContext, String> findSubscriptionWithResilience(UpgradeContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<UpgradeContext, String>success(new UpgradeContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.newTier(), subscription, null)))
                .orElse(Result.<UpgradeContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<UpgradeContext, String> validateCanUpgrade(UpgradeContext context) {
        return switch (context.subscription().getStatus()) {
            case ACTIVE, TRIAL -> validateTierUpgrade(context);
            case PENDING -> Result.failure("Cannot upgrade pending subscription");
            case CANCELLED, EXPIRED -> Result.failure("Cannot upgrade cancelled or expired subscription");
            case SUSPENDED -> Result.failure("Cannot upgrade suspended subscription");
            default -> Result.failure("Invalid subscription status for upgrade: " + context.subscription().getStatus());
        };
    }
    
    private Result<UpgradeContext, String> validateTierUpgrade(UpgradeContext context) {
        SubscriptionTier currentTier = context.subscription().getTier();
        SubscriptionTier newTier = context.newTier();
        
        return switch (compareTiers(currentTier, newTier)) {
            case "UPGRADE" -> Result.success(context);
            case "SAME" -> Result.failure("Already on requested tier: " + newTier);
            case "DOWNGRADE" -> Result.failure("Cannot downgrade from " + currentTier + " to " + newTier);
            default -> Result.failure("Invalid tier upgrade request");
        };
    }
    
    private String compareTiers(SubscriptionTier current, SubscriptionTier target) {
        int currentRank = getTierRank(current);
        int targetRank = getTierRank(target);
        
        return switch (Integer.compare(targetRank, currentRank)) {
            case 1 -> "UPGRADE";
            case 0 -> "SAME";
            case -1 -> "DOWNGRADE";
            default -> "INVALID";
        };
    }
    
    private int getTierRank(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> 1;
            case PRO -> 2;
            case AI_PREMIUM -> 3;
            case INSTITUTIONAL -> 4;
        };
    }
    
    private Result<UpgradeContext, String> applyTierUpgrade(UpgradeContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            SubscriptionTier previousTier = subscription.getTier();
            
            subscription.setTier(context.newTier());
            subscription.setUpgradedDate(LocalDateTime.now());
            subscription.updateNextBillingDate();
            
            log.debug("Applied tier upgrade for subscription: {} from {} to {}", 
                subscription.getId(), previousTier, context.newTier());
                
            return new UpgradeContext(
                context.correlationId(), context.subscriptionId(), 
                context.newTier(), subscription, previousTier
            );
        }).mapError(exception -> "Failed to apply tier upgrade: " + exception.getMessage());
    }
    
    private Result<Subscription, String> saveUpgradedSubscription(UpgradeContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> subscriptionRepository.save(context.subscription()))
                .mapError(exception -> "Failed to save upgraded subscription: " + exception.getMessage())
        );
    }
    
    private Result<Subscription, String> updateUsageLimitsForNewTier(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                List<UsageTracking> usageRecords = usageTrackingRepository
                    .findBySubscriptionId(subscription.getId());
                
                List<UsageTracking> updatedRecords = usageRecords.stream()
                    .map(usage -> {
                        String feature = usage.getFeature();
                        Long newLimit = subscription.getTier().getUsageLimit(feature);
                        usage.setUsageLimit(newLimit);
                        return usage;
                    })
                    .toList();
                
                usageTrackingRepository.saveAll(updatedRecords);
                
                log.debug("Updated usage limits for subscription: {}", subscription.getId());
                return subscription;
            }).mapError(exception -> "Failed to update usage limits: " + exception.getMessage())
        );
    }
    
    private Result<Subscription, String> recordUpgradeHistory(UpgradeContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(context.subscription().getId())
                    .userId(context.subscription().getUserId())
                    .action("SUBSCRIPTION_UPGRADED")
                    .oldTier(context.previousTier())
                    .newTier(context.newTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription tier upgraded")
                    .initiatedBy(SubscriptionHistory.InitiatedBy.USER)
                    .build();
                
                historyRepository.save(history);
                return context.subscription();
            }).mapError(exception -> "Failed to record upgrade history: " + exception.getMessage())
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
    
    // Logging methods
    private void logSuccessfulUpgrade(Subscription subscription, String correlationId) {
        log.info("Subscription upgraded successfully: {}", subscription.getId());
    }
    
    private void logUpgradeFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to upgrade subscription: {}, error: {}", subscriptionId, error);
    }
    
    // Context Record for Functional Operations
    private record UpgradeContext(
        String correlationId,
        UUID subscriptionId,
        SubscriptionTier newTier,
        Subscription subscription,
        SubscriptionTier previousTier
    ) {}
}