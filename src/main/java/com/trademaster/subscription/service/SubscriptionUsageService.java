package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import com.trademaster.subscription.service.interfaces.ISubscriptionUsageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Subscription Usage Service
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription usage tracking operations only
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUsageService implements ISubscriptionUsageService {

    private final SubscriptionRepository subscriptionRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final SubscriptionMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    
    // Circuit Breakers for Resilience
    private final CircuitBreaker databaseCircuitBreaker;
    private final Retry databaseRetry;

    /**
     * Check if feature usage is allowed for subscription
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Boolean, String>> canUseFeature(
            UUID subscriptionId, String featureName) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeUsageCheckContext(subscriptionId, featureName, correlationId)
                .flatMap(this::findSubscriptionWithResilience)
                .flatMap(this::checkFeatureUsageLimit)
                .map(context -> context.canUse())
                .onSuccess(canUse -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "can_use_feature");
                    logUsageCheck(subscriptionId, featureName, canUse, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "can_use_feature_failed");
                    logUsageCheckFailure(subscriptionId, featureName, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Increment usage for a feature
     */
    @Override
    @Transactional
    public CompletableFuture<Result<UsageTracking, String>> incrementUsage(
            UUID subscriptionId, String featureName, int incrementBy) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return initializeUsageIncrementContext(subscriptionId, featureName, incrementBy, correlationId)
                .flatMap(this::findSubscriptionForIncrement)
                .flatMap(this::validateUsageIncrement)
                .flatMap(this::applyUsageIncrement)
                .flatMap(this::saveUsageTracking)
                .onSuccess(usage -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "increment_usage");
                    logUsageIncrement(subscriptionId, featureName, incrementBy, correlationId);
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "increment_usage_failed");
                    logUsageIncrementFailure(subscriptionId, featureName, error, correlationId);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Get current usage for subscription
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<UsageTracking>, String>> getCurrentUsage(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> 
                Result.tryExecute(() -> usageTrackingRepository.findBySubscriptionId(subscriptionId))
                    .mapError(exception -> "Failed to get current usage: " + exception.getMessage())),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Reset usage for billing period
     */
    @Override
    @Transactional
    public CompletableFuture<Result<List<UsageTracking>, String>> resetUsageForBillingPeriod(
            UUID subscriptionId) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return executeWithResilience(() -> 
                Result.tryExecute(() -> {
                    List<UsageTracking> usageRecords = usageTrackingRepository.findBySubscriptionId(subscriptionId);
                    
                    List<UsageTracking> resetRecords = usageRecords.stream()
                        .map(usage -> {
                            usage.setUsageCount(0L);
                            usage.setLastResetDate(LocalDateTime.now());
                            return usage;
                        })
                        .toList();
                    
                    return usageTrackingRepository.saveAll(resetRecords);
                }).mapError(exception -> "Failed to reset usage: " + exception.getMessage())
            ).onSuccess(records -> {
                metricsService.recordSubscriptionProcessingTime(timer, "reset_usage");
                log.info("Reset usage for {} features in subscription: {}", records.size(), subscriptionId);
            }).onFailure(error -> {
                metricsService.recordSubscriptionProcessingTime(timer, "reset_usage_failed");
                log.error("Failed to reset usage for subscription: {}, error: {}", subscriptionId, error);
            });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    // Private helper methods with functional programming patterns
    
    private Result<UsageCheckContext, String> initializeUsageCheckContext(
            UUID subscriptionId, String featureName, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.debug("Checking usage for subscription: {}, feature: {}", subscriptionId, featureName);
            
            return new UsageCheckContext(correlationId, subscriptionId, featureName, null, false);
        }).mapError(exception -> "Failed to initialize usage check context: " + exception.getMessage());
    }
    
    private Result<UsageIncrementContext, String> initializeUsageIncrementContext(
            UUID subscriptionId, String featureName, int incrementBy, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.debug("Incrementing usage for subscription: {}, feature: {}, by: {}", 
                    subscriptionId, featureName, incrementBy);
            
            return new UsageIncrementContext(correlationId, subscriptionId, featureName, incrementBy, null, null);
        }).mapError(exception -> "Failed to initialize usage increment context: " + exception.getMessage());
    }
    
    private Result<UsageCheckContext, String> findSubscriptionWithResilience(UsageCheckContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<UsageCheckContext, String>success(new UsageCheckContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.featureName(), subscription, false)))
                .orElse(Result.<UsageCheckContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<UsageIncrementContext, String> findSubscriptionForIncrement(UsageIncrementContext context) {
        return executeWithResilience(() -> {
            Result<Optional<Subscription>, Exception> repoResult = Result.tryExecute(() -> subscriptionRepository.findById(context.subscriptionId()));
            Result<Optional<Subscription>, String> mappedResult = repoResult.mapError(Exception::getMessage);
            return mappedResult.flatMap(subscriptionOpt -> subscriptionOpt
                .map(subscription -> Result.<UsageIncrementContext, String>success(new UsageIncrementContext(
                    context.correlationId(), context.subscriptionId(), 
                    context.featureName(), context.incrementBy(), subscription, null)))
                .orElse(Result.<UsageIncrementContext, String>failure("Subscription not found: " + context.subscriptionId())));
        });
    }
    
    private Result<UsageCheckContext, String> checkFeatureUsageLimit(UsageCheckContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                UsageTracking usage = usageTrackingRepository
                    .findBySubscriptionIdAndFeature(context.subscriptionId(), context.featureName())
                    .orElse(createDefaultUsageTracking(context.subscriptionId(), context.featureName(), context.subscription().getTier()));
                
                boolean canUse = switch (hasExceededLimit(usage)) {
                    case true -> false;
                    case false -> true;
                };
                
                return new UsageCheckContext(
                    context.correlationId(), context.subscriptionId(),
                    context.featureName(), context.subscription(), canUse
                );
            }).mapError(exception -> "Failed to check feature usage limit: " + exception.getMessage())
        );
    }
    
    private Result<UsageIncrementContext, String> validateUsageIncrement(UsageIncrementContext context) {
        return executeWithResilience(() -> {
            try {
                UsageTracking currentUsage = usageTrackingRepository
                    .findBySubscriptionIdAndFeature(context.subscriptionId(), context.featureName())
                    .orElse(createDefaultUsageTracking(context.subscriptionId(), context.featureName(), context.subscription().getTier()));
                
                return switch (wouldExceedLimit(currentUsage, context.incrementBy())) {
                    case true -> Result.<UsageIncrementContext, String>failure("Usage increment would exceed limit for feature: " + context.featureName());
                    case false -> Result.<UsageIncrementContext, String>success(new UsageIncrementContext(
                        context.correlationId(), context.subscriptionId(),
                        context.featureName(), context.incrementBy(), context.subscription(), currentUsage
                    ));
                };
            } catch (Exception exception) {
                return Result.<UsageIncrementContext, String>failure("Failed to validate usage increment: " + exception.getMessage());
            }
        });
    }
    
    private Result<UsageIncrementContext, String> applyUsageIncrement(UsageIncrementContext context) {
        return Result.tryExecute(() -> {
            UsageTracking usage = context.currentUsage();
            usage.setUsageCount(usage.getUsageCount() + context.incrementBy());
            usage.setLastUsedDate(LocalDateTime.now());
            
            return new UsageIncrementContext(
                context.correlationId(), context.subscriptionId(),
                context.featureName(), context.incrementBy(), context.subscription(), usage
            );
        }).mapError(exception -> "Failed to apply usage increment: " + exception.getMessage());
    }
    
    private Result<UsageTracking, String> saveUsageTracking(UsageIncrementContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> usageTrackingRepository.save(context.currentUsage()))
                .mapError(exception -> "Failed to save usage tracking: " + exception.getMessage())
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
    
    // Helper methods using pattern matching
    private boolean hasExceededLimit(UsageTracking usage) {
        return switch (Long.compare(usage.getUsageCount(), usage.getUsageLimit())) {
            case 1 -> true;  // usage > limit
            case 0, -1 -> false;  // usage <= limit
            default -> false;
        };
    }
    
    private boolean wouldExceedLimit(UsageTracking usage, int increment) {
        long newUsage = usage.getUsageCount() + increment;
        return switch (Long.compare(newUsage, usage.getUsageLimit())) {
            case 1 -> true;  // would exceed
            case 0, -1 -> false;  // would not exceed
            default -> false;
        };
    }
    
    private UsageTracking createDefaultUsageTracking(UUID subscriptionId, String featureName, SubscriptionTier tier) {
        return UsageTracking.builder()
            .subscriptionId(subscriptionId)
            .featureName(featureName)
            .usageCount(0L)
            .usageLimit(getFeatureLimitForTier(featureName, tier))
            .periodStart(LocalDateTime.now().withDayOfMonth(1))
            .periodEnd(LocalDateTime.now().withDayOfMonth(1).plusMonths(1))
            .resetDate(LocalDateTime.now())
            .build();
    }
    
    private Long getFeatureLimitForTier(String featureName, SubscriptionTier tier) {
        return switch (featureName) {
            case "api_calls" -> switch (tier) {
                case FREE -> 1000L;
                case PRO -> 10000L;
                case AI_PREMIUM -> 50000L;
                case INSTITUTIONAL -> -1L; // Unlimited
            };
            case "portfolios" -> switch (tier) {
                case FREE -> 3L;
                case PRO -> 10L;
                case AI_PREMIUM -> 50L;
                case INSTITUTIONAL -> -1L;
            };
            case "watchlists" -> switch (tier) {
                case FREE -> 5L;
                case PRO -> 25L;
                case AI_PREMIUM -> 100L;
                case INSTITUTIONAL -> -1L;
            };
            case "alerts" -> switch (tier) {
                case FREE -> 10L;
                case PRO -> 100L;
                case AI_PREMIUM -> 500L;
                case INSTITUTIONAL -> -1L;
            };
            case "ai_insights" -> switch (tier) {
                case FREE -> 0L;
                case PRO -> 0L;
                case AI_PREMIUM -> 1000L;
                case INSTITUTIONAL -> -1L;
            };
            default -> 0L;
        };
    }
    
    // Logging methods
    private void logUsageCheck(UUID subscriptionId, String featureName, boolean canUse, String correlationId) {
        log.debug("Usage check completed: subscription={}, feature={}, canUse={}", 
                subscriptionId, featureName, canUse);
    }
    
    private void logUsageCheckFailure(UUID subscriptionId, String featureName, String error, String correlationId) {
        log.error("Usage check failed: subscription={}, feature={}, error={}", 
                subscriptionId, featureName, error);
    }
    
    private void logUsageIncrement(UUID subscriptionId, String featureName, int incrementBy, String correlationId) {
        log.debug("Usage incremented: subscription={}, feature={}, increment={}", 
                subscriptionId, featureName, incrementBy);
    }
    
    private void logUsageIncrementFailure(UUID subscriptionId, String featureName, String error, String correlationId) {
        log.error("Usage increment failed: subscription={}, feature={}, error={}", 
                subscriptionId, featureName, error);
    }
    
    // Context Records for Functional Operations
    private record UsageCheckContext(
        String correlationId,
        UUID subscriptionId,
        String featureName,
        Subscription subscription,
        boolean canUse
    ) {}
    
    private record UsageIncrementContext(
        String correlationId,
        UUID subscriptionId,
        String featureName,
        int incrementBy,
        Subscription subscription,
        UsageTracking currentUsage
    ) {}
}