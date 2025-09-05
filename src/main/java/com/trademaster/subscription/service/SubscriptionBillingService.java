package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.service.interfaces.ISubscriptionBillingService;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
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
 * Subscription Billing Service
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription billing operations only
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingService implements ISubscriptionBillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final SubscriptionMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    
    // Circuit Breakers for Resilience
    private final CircuitBreaker databaseCircuitBreaker;
    private final Retry databaseRetry;

    /**
     * Process billing for subscription
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> processBilling(
            UUID subscriptionId, UUID paymentTransactionId) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeBillingContext(subscriptionId, paymentTransactionId, correlationId)
                .flatMap(this::findSubscriptionWithResilience)
                .flatMap(this::validateCanProcessBilling)
                .flatMap(this::calculateBillingAmount)
                .flatMap(this::processBillingPayment)
                .flatMap(this::updateNextBillingDate)
                .flatMap(this::saveBilledSubscription)
                .flatMap(this::recordBillingHistory)
                .map(context -> context.subscription())
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "process_billing");
                    logSuccessfulBilling(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "process_billing_failed");
                    logBillingFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Update billing cycle for subscription
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> updateBillingCycle(
            UUID subscriptionId, BillingCycle newBillingCycle) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeBillingCycleContext(subscriptionId, newBillingCycle, correlationId)
                .flatMap(this::findSubscriptionForBillingCycle)
                .flatMap(this::validateBillingCycleChange)
                .flatMap(this::updateBillingCycleAndAmount)
                .flatMap(this::saveBillingCycleChange)
                .flatMap(this::recordBillingCycleHistory)
                .map(context -> context.subscription())
                .onSuccess(subscription -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "update_billing_cycle");
                    logSuccessfulBillingCycleUpdate(subscription, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "update_billing_cycle_failed");
                    logBillingCycleUpdateFailure(subscriptionId, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get upcoming billing amount for subscription
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<BigDecimal, String>> getUpcomingBillingAmount(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> subscriptionRepository.findById(subscriptionId))
                    .mapError(exception -> "Failed to get billing amount: " + exception.getMessage())
                    .flatMap(subscriptionOpt -> subscriptionOpt
                        .map(subscription -> Result.<BigDecimal, String>success(calculateBillingAmount(subscription.getTier(), subscription.getBillingCycle())))
                        .orElse(Result.<BigDecimal, String>failure("Subscription not found: " + subscriptionId)))),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get all subscriptions due for billing
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<Subscription>, String>> getSubscriptionsDueForBilling() {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> subscriptionRepository.findSubscriptionsDueForBilling(LocalDateTime.now()))
                    .mapError(exception -> "Failed to get subscriptions due for billing: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Private helper methods with functional programming patterns
    
    private Result<BillingContext, String> initializeBillingContext(
            UUID subscriptionId, UUID paymentTransactionId, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.info("Processing billing for subscription: {} with transaction: {}", 
                    subscriptionId, paymentTransactionId);
            
            return new BillingContext(correlationId, subscriptionId, paymentTransactionId, null, null, null);
        }).mapError(exception -> "Failed to initialize billing context: " + exception.getMessage());
    }
    
    private Result<BillingCycleContext, String> initializeBillingCycleContext(
            UUID subscriptionId, BillingCycle newBillingCycle, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.info("Updating billing cycle for subscription: {} to: {}", 
                    subscriptionId, newBillingCycle);
            
            return new BillingCycleContext(correlationId, subscriptionId, newBillingCycle, null, null);
        }).mapError(exception -> "Failed to initialize billing cycle context: " + exception.getMessage());
    }
    
    private Result<BillingContext, String> findSubscriptionWithResilience(BillingContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<BillingContext, String>success(new BillingContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.paymentTransactionId(), subscription, null, null)))
                .orElse(Result.<BillingContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<BillingCycleContext, String> findSubscriptionForBillingCycle(BillingCycleContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<BillingCycleContext, String>success(new BillingCycleContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.newBillingCycle(), subscription, subscription.getBillingCycle())))
                .orElse(Result.<BillingCycleContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<BillingContext, String> validateCanProcessBilling(BillingContext context) {
        return switch (context.subscription().getStatus()) {
            case ACTIVE -> Result.success(context);
            case TRIAL -> Result.failure("Cannot bill trial subscription");
            case PENDING -> Result.failure("Cannot bill pending subscription");
            case CANCELLED, EXPIRED -> Result.failure("Cannot bill cancelled or expired subscription");
            case SUSPENDED -> Result.failure("Cannot bill suspended subscription");
            default -> Result.failure("Invalid subscription status for billing: " + context.subscription().getStatus());
        };
    }
    
    private Result<BillingCycleContext, String> validateBillingCycleChange(BillingCycleContext context) {
        return switch (context.subscription().getStatus()) {
            case ACTIVE, TRIAL -> validateBillingCycleCompatibility(context);
            case PENDING -> Result.failure("Cannot change billing cycle for pending subscription");
            case CANCELLED, EXPIRED -> Result.failure("Cannot change billing cycle for cancelled or expired subscription");
            case SUSPENDED -> Result.failure("Cannot change billing cycle for suspended subscription");
            default -> Result.failure("Invalid subscription status for billing cycle change");
        };
    }
    
    private Result<BillingCycleContext, String> validateBillingCycleCompatibility(BillingCycleContext context) {
        BillingCycle currentCycle = context.oldBillingCycle();
        BillingCycle newCycle = context.newBillingCycle();
        
        return switch (currentCycle == newCycle ? "SAME" : "DIFFERENT") {
            case "SAME" -> Result.failure("Already on requested billing cycle: " + newCycle);
            case "DIFFERENT" -> Result.success(context);
            default -> Result.failure("Invalid billing cycle change request");
        };
    }
    
    private Result<BillingContext, String> calculateBillingAmount(BillingContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            BigDecimal amount = calculateBillingAmount(subscription.getTier(), subscription.getBillingCycle());
            
            return new BillingContext(
                context.correlationId(), context.subscriptionId(),
                context.paymentTransactionId(), subscription, amount, null
            );
        }).mapError(exception -> "Failed to calculate billing amount: " + exception.getMessage());
    }
    
    private Result<BillingContext, String> processBillingPayment(BillingContext context) {
        return Result.tryExecute(() -> {
            // Process payment logic would go here
            // For now, we'll mark as processed
            
            LocalDateTime billingDate = LocalDateTime.now();
            
            return new BillingContext(
                context.correlationId(), context.subscriptionId(),
                context.paymentTransactionId(), context.subscription(),
                context.billingAmount(), billingDate
            );
        }).mapError(exception -> "Failed to process billing payment: " + exception.getMessage());
    }
    
    private Result<BillingContext, String> updateNextBillingDate(BillingContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            subscription.setLastBilledDate(context.billingDate());
            subscription.updateNextBillingDate();
            
            return new BillingContext(
                context.correlationId(), context.subscriptionId(),
                context.paymentTransactionId(), subscription,
                context.billingAmount(), context.billingDate()
            );
        }).mapError(exception -> "Failed to update next billing date: " + exception.getMessage());
    }
    
    private Result<BillingCycleContext, String> updateBillingCycleAndAmount(BillingCycleContext context) {
        return Result.tryExecute(() -> {
            Subscription subscription = context.subscription();
            subscription.setBillingCycle(context.newBillingCycle());
            
            BigDecimal newAmount = calculateBillingAmount(subscription.getTier(), context.newBillingCycle());
            subscription.setBillingAmount(newAmount);
            subscription.updateNextBillingDate();
            
            return new BillingCycleContext(
                context.correlationId(), context.subscriptionId(),
                context.newBillingCycle(), subscription, context.oldBillingCycle()
            );
        }).mapError(exception -> "Failed to update billing cycle: " + exception.getMessage());
    }
    
    private Result<BillingContext, String> saveBilledSubscription(BillingContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                subscriptionRepository.save(context.subscription());
                return context;
            }).mapError(exception -> "Failed to save billed subscription: " + exception.getMessage())
        );
    }
    
    private Result<BillingCycleContext, String> saveBillingCycleChange(BillingCycleContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                subscriptionRepository.save(context.subscription());
                return context;
            }).mapError(exception -> "Failed to save billing cycle change: " + exception.getMessage())
        );
    }
    
    private Result<BillingContext, String> recordBillingHistory(BillingContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(context.subscription().getId())
                    .userId(context.subscription().getUserId())
                    .action("SUBSCRIPTION_BILLED")
                    .oldTier(context.subscription().getTier())
                    .newTier(context.subscription().getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Subscription billed successfully")
                    .initiatedBy(SubscriptionHistory.InitiatedBy.SYSTEM)
                    .build();
                
                historyRepository.save(history);
                return context;
            }).mapError(exception -> "Failed to record billing history: " + exception.getMessage())
        );
    }
    
    private Result<BillingCycleContext, String> recordBillingCycleHistory(BillingCycleContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionHistory history = SubscriptionHistory.builder()
                    .subscriptionId(context.subscription().getId())
                    .userId(context.subscription().getUserId())
                    .action("BILLING_CYCLE_CHANGED")
                    .oldTier(context.subscription().getTier())
                    .newTier(context.subscription().getTier())
                    .effectiveDate(LocalDateTime.now())
                    .changeReason("Billing cycle changed from " + context.oldBillingCycle() + " to " + context.newBillingCycle())
                    .initiatedBy(SubscriptionHistory.InitiatedBy.USER)
                    .build();
                
                historyRepository.save(history);
                return context;
            }).mapError(exception -> "Failed to record billing cycle history: " + exception.getMessage())
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
    
    // Pattern matching helper for billing amount calculation
    private BigDecimal calculateBillingAmount(SubscriptionTier tier, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUAL -> tier.getAnnualPrice();
        };
    }
    
    // Logging methods
    private void logSuccessfulBilling(Subscription subscription, String correlationId) {
        log.info("Subscription billed successfully: {}", subscription.getId());
    }
    
    private void logBillingFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to bill subscription: {}, error: {}", subscriptionId, error);
    }
    
    private void logSuccessfulBillingCycleUpdate(Subscription subscription, String correlationId) {
        log.info("Billing cycle updated successfully for subscription: {}", subscription.getId());
    }
    
    private void logBillingCycleUpdateFailure(UUID subscriptionId, String error, String correlationId) {
        log.error("Failed to update billing cycle for subscription: {}, error: {}", subscriptionId, error);
    }
    
    // Context Records for Functional Operations
    private record BillingContext(
        String correlationId,
        UUID subscriptionId,
        UUID paymentTransactionId,
        Subscription subscription,
        BigDecimal billingAmount,
        LocalDateTime billingDate
    ) {}
    
    private record BillingCycleContext(
        String correlationId,
        UUID subscriptionId,
        BillingCycle newBillingCycle,
        Subscription subscription,
        BillingCycle oldBillingCycle
    ) {}
}