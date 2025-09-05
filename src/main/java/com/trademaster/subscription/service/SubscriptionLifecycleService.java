package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.service.interfaces.ISubscriptionLifecycleService;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Lifecycle Service
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription lifecycle operations only
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleService implements ISubscriptionLifecycleService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final SubscriptionMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    
    // Circuit Breakers for Resilience
    private final CircuitBreaker databaseCircuitBreaker;
    private final Retry databaseRetry;
    
    // Active statuses for queries
    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(
        SubscriptionStatus.ACTIVE, 
        SubscriptionStatus.TRIAL, 
        SubscriptionStatus.EXPIRED
    );

    /**
     * Create a new subscription
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> createSubscription(
            UUID userId, SubscriptionTier tier, BillingCycle billingCycle, boolean startTrial) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeCreationContext(userId, tier, billingCycle, startTrial, correlationId)
                .flatMap(this::validateNoExistingSubscription)
                .flatMap(this::createSubscriptionEntity)
                .flatMap(this::saveSubscriptionWithResilience)
                .flatMap(this::recordCreationHistory)
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "create_subscription");
                    logSuccessfulCreation(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "create_subscription_failed");
                    logCreationFailure(userId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Activate subscription after payment
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> activateSubscription(
            UUID subscriptionId, UUID paymentTransactionId) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeActivationContext(subscriptionId, paymentTransactionId, correlationId)
                .flatMap(this::findSubscriptionWithResilience)
                .flatMap(this::validateCanActivate)
                .flatMap(this::performActivation)
                .flatMap(this::saveActivatedSubscription)
                .flatMap(this::recordActivationHistory)
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "activate_subscription");
                    logSuccessfulActivation(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "activate_subscription_failed");
                    logActivationFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Cancel subscription
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
                    metricsService.recordSubscriptionProcessingTime(timer, "cancel_subscription");
                    logSuccessfulCancellation(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "cancel_subscription_failed");
                    logCancellationFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get active subscription for user
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> getActiveSubscription(UUID userId) {
        return CompletableFuture.supplyAsync(() -> 
            Result.tryExecute(() -> subscriptionRepository.findActiveByUserId(userId, ACTIVE_STATUSES))
                .mapError(exception -> "Failed to retrieve active subscription: " + exception.getMessage()),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Private helper methods with functional programming patterns
    
    private Result<CreationContext, String> initializeCreationContext(
            UUID userId, SubscriptionTier tier, BillingCycle billingCycle, 
            boolean startTrial, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            loggingService.setUserContext(userId.toString(), null, null, null);
            
            log.info("Creating subscription for user: {}, tier: {}, cycle: {}", 
                    userId, tier, billingCycle);
            
            return new CreationContext(correlationId, userId, tier, billingCycle, startTrial, null);
        }).mapError(exception -> "Failed to initialize creation context: " + exception.getMessage());
    }
    
    private Result<CreationContext, String> validateNoExistingSubscription(CreationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> subscriptionRepository.findActiveByUserId(context.userId(), ACTIVE_STATUSES))
                .mapError(Exception::getMessage)
                .flatMap(existingSubscription -> existingSubscription.isEmpty() 
                    ? Result.success(context)
                    : Result.failure("User already has an active subscription"))
        );
    }
    
    private Result<CreationContext, String> createSubscriptionEntity(CreationContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = Subscription.builder()
                .userId(context.userId())
                .tier(context.tier())
                .status(context.startTrial() ? SubscriptionStatus.TRIAL : SubscriptionStatus.PENDING)
                .billingCycle(context.billingCycle())
                .monthlyPrice(context.tier().getMonthlyPrice())
                .billingAmount(calculateBillingAmount(context.tier(), context.billingCycle()))
                .currency("INR")
                .startDate(LocalDateTime.now())
                .autoRenewal(true)
                .build();
            
            // Set trial end date using pattern matching
            switch (context.startTrial() ? "TRIAL" : "REGULAR") {
                case "TRIAL" -> subscription.setTrialEndDate(LocalDateTime.now().plusDays(7));
                case "REGULAR" -> { /* No trial end date needed */ }
            }
            
            subscription.updateNextBillingDate();
            
            return new CreationContext(
                context.correlationId(), context.userId(), context.tier(), 
                context.billingCycle(), context.startTrial(), subscription
            );
        }).mapError(exception -> "Failed to create subscription entity: " + exception.getMessage());
    }
    
    private Result<Subscription, String> saveSubscriptionWithResilience(CreationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> subscriptionRepository.save(context.subscription()))
                .mapError(exception -> "Failed to save subscription: " + exception.getMessage())
        );
    }
    
    private Result<Subscription, String> recordCreationHistory(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUserId())
                    .action("SUBSCRIPTION_CREATED")
                    .oldTier(null)
                    .newTier(subscription.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Initial subscription creation")
                    .initiatedBy(SubscriptionHistory.InitiatedBy.SYSTEM)
                    .build();
                
                historyRepository.save(history);
                return subscription;
            }).mapError(exception -> "Failed to record creation history: " + exception.getMessage())
        );
    }
    
    // Helper methods for other operations
    private Result<ActivationContext, String> initializeActivationContext(
            UUID subscriptionId, UUID paymentTransactionId, String correlationId) {
        return Result.success(new ActivationContext(correlationId, subscriptionId, paymentTransactionId, null));
    }
    
    private Result<ActivationContext, String> findSubscriptionWithResilience(ActivationContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<ActivationContext, String>success(new ActivationContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.paymentTransactionId(), subscription)))
                .orElse(Result.<ActivationContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<CancellationContext, String> findSubscriptionForCancellation(CancellationContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<CancellationContext, String>success(new CancellationContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.cancellationReason(), subscription)))
                .orElse(Result.<CancellationContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
    
    // Pattern matching helper
    private BigDecimal calculateBillingAmount(SubscriptionTier tier, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUAL -> tier.getAnnualPrice();
        };
    }
    
    // Logging methods
    private void logSuccessfulCreation(Subscription subscription, String correlationId) {
        log.info("Subscription created successfully: {}", subscription.getId());
    }
    
    private void logCreationFailure(UUID userId, String error, String correlationId) {
        log.error("Failed to create subscription for user: {}, error: {}", userId, error);
    }
    
    private void logSuccessfulActivation(Subscription subscription, String correlationId) {
        log.info("Subscription activated successfully: {}", subscription.getId());
    }
    
    private void logActivationFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to activate subscription: {}, error: {}", subscriptionId, error);
    }
    
    private void logSuccessfulCancellation(Subscription subscription, String correlationId) {
        log.info("Subscription cancelled successfully: {}", subscription.getId());
    }
    
    private void logCancellationFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to cancel subscription: {}, error: {}", subscriptionId, error);
    }
    
    // Stub methods for remaining operations (to be implemented)
    private Result<CancellationContext, String> initializeCancellationContext(
            UUID subscriptionId, String cancellationReason, String correlationId) {
        return Result.success(new CancellationContext(correlationId, subscriptionId, cancellationReason, null));
    }
    
    private Result<ActivationContext, String> validateCanActivate(ActivationContext context) {
        return switch (context.subscription().getStatus()) {
            case PENDING, TRIAL -> Result.success(context);
            case ACTIVE -> Result.failure("Subscription is already active");
            case CANCELLED, EXPIRED -> Result.failure("Cannot activate cancelled or expired subscription");
            case SUSPENDED -> Result.failure("Cannot activate suspended subscription");
            default -> Result.failure("Invalid subscription status for activation");
        };
    }
    
    private Result<Subscription, String> performActivation(ActivationContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setActivatedDate(LocalDateTime.now());
            subscription.updateNextBillingDate();
            return subscription;
        }).mapError(exception -> "Failed to perform activation: " + exception.getMessage());
    }
    
    private Result<Subscription, String> saveActivatedSubscription(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> subscriptionRepository.save(subscription))
                .mapError(exception -> "Failed to save activated subscription: " + exception.getMessage())
        );
    }
    
    private Result<Subscription, String> recordActivationHistory(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUserId())
                    .action("SUBSCRIPTION_ACTIVATED")
                    .oldTier(subscription.getTier())
                    .newTier(subscription.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription activated after successful payment")
                    .initiatedBy(SubscriptionHistory.InitiatedBy.SYSTEM)
                    .build();
                
                historyRepository.save(history);
                return subscription;
            }).mapError(exception -> "Failed to record activation history: " + exception.getMessage())
        );
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
            subscription.setCancelledDate(LocalDateTime.now());
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
                    .action("SUBSCRIPTION_CANCELLED")
                    .oldTier(subscription.getTier())
                    .newTier(subscription.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription cancelled by user request")
                    .initiatedBy(SubscriptionHistory.InitiatedBy.USER)
                    .build();
                
                historyRepository.save(history);
                return subscription;
            }).mapError(exception -> "Failed to record cancellation history: " + exception.getMessage())
        );
    }
    
    // Context Records for Functional Operations
    private record CreationContext(
        String correlationId,
        UUID userId,
        SubscriptionTier tier,
        BillingCycle billingCycle,
        boolean startTrial,
        Subscription subscription
    ) {}
    
    private record ActivationContext(
        String correlationId,
        UUID subscriptionId,
        UUID paymentTransactionId,
        Subscription subscription
    ) {}
    
    private record CancellationContext(
        String correlationId,
        UUID subscriptionId,
        String cancellationReason,
        Subscription subscription
    ) {}
    
    /**
     * Find subscription by ID
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> findById(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> subscriptionRepository.findById(subscriptionId))
                    .mapError(exception -> "Failed to find subscription: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get user subscriptions with pagination
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getUserSubscriptions(UUID userId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> {
                    // Create a custom page query for user subscriptions
                    List<Subscription> userSubscriptions = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
                    int start = Math.toIntExact(pageable.getOffset());
                    int end = Math.min(start + pageable.getPageSize(), userSubscriptions.size());
                    List<Subscription> pageContent = start < userSubscriptions.size() ? 
                        userSubscriptions.subList(start, end) : List.of();
                    
                    return (Page<Subscription>) new org.springframework.data.domain.PageImpl<>(
                        pageContent, pageable, userSubscriptions.size());
                }).mapError(exception -> "Failed to get user subscriptions: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Suspend subscription
     */
    @Override
    @Transactional
    public CompletableFuture<Result<Subscription, String>> suspendSubscription(UUID subscriptionId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeSuspensionContext(subscriptionId, reason, correlationId)
                .flatMap(this::findSubscriptionForSuspension)
                .flatMap(this::validateCanSuspend)
                .flatMap(this::performSuspension)
                .flatMap(this::saveSuspendedSubscription)
                .flatMap(this::recordSuspensionHistory)
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "suspend_subscription");
                    logSuccessfulSuspension(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "suspend_subscription_failed");
                    logSuspensionFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get subscription history
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<SubscriptionHistory>, String>> getSubscriptionHistory(
            UUID subscriptionId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> {
                    // First verify subscription exists
                    if (!subscriptionRepository.existsById(subscriptionId)) {
                        throw new RuntimeException("Subscription not found: " + subscriptionId);
                    }
                    return historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
                }).mapError(exception -> "Failed to get subscription history: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Check subscription health
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<String, String>> checkSubscriptionHealth(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> {
                    Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
                    if (subscriptionOpt.isEmpty()) {
                        throw new RuntimeException("Subscription not found: " + subscriptionId);
                    }
                    
                    Subscription subscription = subscriptionOpt.get();
                    return switch (subscription.getStatus()) {
                        case ACTIVE, TRIAL -> "healthy";
                        case SUSPENDED, EXPIRED -> "unhealthy";
                        case CANCELLED -> "unhealthy";
                        default -> "unhealthy";
                    };
                }).mapError(exception -> "Failed to check subscription health: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get subscriptions by status
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getSubscriptionsByStatus(
            SubscriptionStatus status, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> {
                    List<Subscription> statusSubscriptions = subscriptionRepository.findByStatus(status);
                    int start = Math.toIntExact(pageable.getOffset());
                    int end = Math.min(start + pageable.getPageSize(), statusSubscriptions.size());
                    List<Subscription> pageContent = start < statusSubscriptions.size() ? 
                        statusSubscriptions.subList(start, end) : List.of();
                    
                    return (Page<Subscription>) new org.springframework.data.domain.PageImpl<>(
                        pageContent, pageable, statusSubscriptions.size());
                }).mapError(exception -> "Failed to get subscriptions by status: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Additional helper methods for suspension
    private Result<SuspensionContext, String> initializeSuspensionContext(
            UUID subscriptionId, String reason, String correlationId) {
        return Result.success(new SuspensionContext(correlationId, subscriptionId, reason, null));
    }
    
    private Result<SuspensionContext, String> findSubscriptionForSuspension(SuspensionContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<SuspensionContext, String>success(new SuspensionContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.reason(), subscription)))
                .orElse(Result.<SuspensionContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<SuspensionContext, String> validateCanSuspend(SuspensionContext context) {
        return switch (context.subscription().getStatus()) {
            case ACTIVE, TRIAL, EXPIRED -> Result.success(context);
            case SUSPENDED -> Result.failure("Subscription is already suspended");
            case CANCELLED -> Result.failure("Cannot suspend cancelled subscription");
            case PENDING -> Result.failure("Cannot suspend pending subscription");
            default -> Result.failure("Invalid subscription status for suspension");
        };
    }
    
    private Result<Subscription, String> performSuspension(SuspensionContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            return subscription;
        }).mapError(exception -> "Failed to perform suspension: " + exception.getMessage());
    }
    
    private Result<Subscription, String> saveSuspendedSubscription(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> subscriptionRepository.save(subscription))
                .mapError(exception -> "Failed to save suspended subscription: " + exception.getMessage())
        );
    }
    
    private Result<Subscription, String> recordSuspensionHistory(Subscription subscription) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUserId())
                    .action("SUBSCRIPTION_SUSPENDED")
                    .oldTier(subscription.getTier())
                    .newTier(subscription.getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription suspended due to: " + subscription.getCancellationReason())
                    .initiatedBy(SubscriptionHistory.InitiatedBy.SYSTEM)
                    .build();
                
                historyRepository.save(history);
                return subscription;
            }).mapError(exception -> "Failed to record suspension history: " + exception.getMessage())
        );
    }
    
    // Logging methods for suspension
    private void logSuccessfulSuspension(Subscription subscription, String correlationId) {
        log.info("Subscription suspended successfully: {}", subscription.getId());
    }
    
    private void logSuspensionFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to suspend subscription: {}, error: {}", subscriptionId, error);
    }
    
    // Suspension Context Record
    private record SuspensionContext(
        String correlationId,
        UUID subscriptionId,
        String reason,
        Subscription subscription
    ) {}
}