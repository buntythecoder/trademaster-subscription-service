package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.service.interfaces.ISubscriptionNotificationService;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Notification Service
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription event publishing and notifications only
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionNotificationService implements ISubscriptionNotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    
    // Circuit Breakers for Resilience
    private final CircuitBreaker notificationCircuitBreaker;
    private final Retry notificationRetry;

    /**
     * Publish subscription created event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCreated(
            Subscription subscription) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createNotificationContext(subscription, "CREATED", correlationId)
                .flatMap(this::publishSubscriptionEvent)
                .flatMap(this::logNotificationSuccess)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_created");
                    log.info("Published subscription created event for: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_created_failed");
                    log.error("Failed to publish subscription created event: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Publish subscription activated event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionActivated(
            Subscription subscription) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createNotificationContext(subscription, "ACTIVATED", correlationId)
                .flatMap(this::publishSubscriptionEvent)
                .flatMap(this::logNotificationSuccess)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_activated");
                    log.info("Published subscription activated event for: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_activated_failed");
                    log.error("Failed to publish subscription activated event: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Publish subscription upgraded event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionUpgraded(
            Subscription subscription, SubscriptionTier previousTier) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createUpgradeNotificationContext(subscription, previousTier, correlationId)
                .flatMap(this::publishSubscriptionUpgradeEvent)
                .flatMap(this::logNotificationSuccess)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_upgraded");
                    log.info("Published subscription upgraded event for: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_upgraded_failed");
                    log.error("Failed to publish subscription upgraded event: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Publish subscription cancelled event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCancelled(
            Subscription subscription, String cancellationReason) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createCancellationNotificationContext(subscription, cancellationReason, correlationId)
                .flatMap(this::publishSubscriptionCancellationEvent)
                .flatMap(this::logNotificationSuccess)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_cancelled");
                    log.info("Published subscription cancelled event for: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_cancelled_failed");
                    log.error("Failed to publish subscription cancelled event: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Publish subscription billing event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionBilled(
            Subscription subscription, UUID transactionId) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createBillingNotificationContext(subscription, transactionId, correlationId)
                .flatMap(this::publishSubscriptionBillingEvent)
                .flatMap(this::logNotificationSuccess)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_billed");
                    log.info("Published subscription billed event for: {}", subscription.getId());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "subscription_billed_failed");
                    log.error("Failed to publish subscription billed event: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Publish batch notifications for multiple subscriptions
     */
    public CompletableFuture<Result<List<UUID>, String>> publishBatchNotifications(
            List<Subscription> subscriptions, String eventType) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();
            
            return createBatchNotificationContext(subscriptions, eventType, correlationId)
                .flatMap(this::processBatchNotifications)
                .onSuccess(result -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "batch_notifications");
                    log.info("Published batch notifications for {} subscriptions", subscriptions.size());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "batch_notifications_failed");
                    log.error("Failed to publish batch notifications: {}", error);
                });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    // Private helper methods with functional programming patterns
    
    private Result<NotificationContext, String> createNotificationContext(
            Subscription subscription, String eventType, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            log.debug("Creating notification context for subscription: {} with event: {}", 
                    subscription.getId(), eventType);
            
            return new NotificationContext(
                correlationId, subscription, eventType, null, null, null, null
            );
        }).mapError(exception -> "Failed to create notification context: " + exception.getMessage());
    }
    
    private Result<NotificationContext, String> createUpgradeNotificationContext(
            Subscription subscription, SubscriptionTier previousTier, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            return new NotificationContext(
                correlationId, subscription, "UPGRADED", previousTier, null, null, null
            );
        }).mapError(exception -> "Failed to create upgrade notification context: " + exception.getMessage());
    }
    
    private Result<NotificationContext, String> createCancellationNotificationContext(
            Subscription subscription, String cancellationReason, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            return new NotificationContext(
                correlationId, subscription, "CANCELLED", null, cancellationReason, null, null
            );
        }).mapError(exception -> "Failed to create cancellation notification context: " + exception.getMessage());
    }
    
    private Result<NotificationContext, String> createBillingNotificationContext(
            Subscription subscription, UUID transactionId, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            return new NotificationContext(
                correlationId, subscription, "BILLED", null, null, transactionId, null
            );
        }).mapError(exception -> "Failed to create billing notification context: " + exception.getMessage());
    }
    
    private Result<BatchNotificationContext, String> createBatchNotificationContext(
            List<Subscription> subscriptions, String eventType, String correlationId) {
        
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(correlationId);
            
            return new BatchNotificationContext(correlationId, subscriptions, eventType);
        }).mapError(exception -> "Failed to create batch notification context: " + exception.getMessage());
    }
    
    private Result<Void, String> publishSubscriptionEvent(NotificationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionEvent event = createSubscriptionEvent(context);
                eventPublisher.publishEvent(event);
                return (Void) null;
            }).mapError(exception -> "Failed to publish subscription event: " + exception.getMessage())
        );
    }
    
    private Result<Void, String> publishSubscriptionUpgradeEvent(NotificationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionUpgradeEvent event = createSubscriptionUpgradeEvent(context);
                eventPublisher.publishEvent(event);
                return (Void) null;
            }).mapError(exception -> "Failed to publish subscription upgrade event: " + exception.getMessage())
        );
    }
    
    private Result<Void, String> publishSubscriptionCancellationEvent(NotificationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionCancellationEvent event = createSubscriptionCancellationEvent(context);
                eventPublisher.publishEvent(event);
                return (Void) null;
            }).mapError(exception -> "Failed to publish subscription cancellation event: " + exception.getMessage())
        );
    }
    
    private Result<Void, String> publishSubscriptionBillingEvent(NotificationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                SubscriptionBillingEvent event = createSubscriptionBillingEvent(context);
                eventPublisher.publishEvent(event);
                return (Void) null;
            }).mapError(exception -> "Failed to publish subscription billing event: " + exception.getMessage())
        );
    }
    
    private Result<List<UUID>, String> processBatchNotifications(BatchNotificationContext context) {
        return executeWithResilience(() -> 
            Result.tryExecute(() -> {
                List<UUID> processedIds = context.subscriptions().stream()
                    .map(subscription -> {
                        SubscriptionEvent event = createSubscriptionEventFromBatch(subscription, context);
                        eventPublisher.publishEvent(event);
                        return subscription.getId();
                    })
                    .toList();
                
                return processedIds;
            }).mapError(exception -> "Failed to process batch notifications: " + exception.getMessage())
        );
    }
    
    private Result<Void, String> logNotificationSuccess(Void result) {
        return Result.tryExecute(() -> {
            log.debug("Notification published successfully");
            return (Void) null;
        }).mapError(exception -> "Failed to log notification success: " + exception.getMessage());
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return notificationCircuitBreaker.executeSupplier(operation);
    }
    
    // Event creation methods using pattern matching
    private SubscriptionEvent createSubscriptionEvent(NotificationContext context) {
        return switch (context.eventType()) {
            case "CREATED" -> new SubscriptionEvent(
                context.subscription().getId(),
                context.subscription().getUserId(),
                SubscriptionEventType.SUBSCRIPTION_CREATED,
                LocalDateTime.now(),
                context.correlationId()
            );
            case "ACTIVATED" -> new SubscriptionEvent(
                context.subscription().getId(),
                context.subscription().getUserId(),
                SubscriptionEventType.SUBSCRIPTION_ACTIVATED,
                LocalDateTime.now(),
                context.correlationId()
            );
            default -> throw new IllegalArgumentException("Unknown event type: " + context.eventType());
        };
    }
    
    private SubscriptionUpgradeEvent createSubscriptionUpgradeEvent(NotificationContext context) {
        return new SubscriptionUpgradeEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.previousTier(),
            context.subscription().getTier(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionCancellationEvent createSubscriptionCancellationEvent(NotificationContext context) {
        return new SubscriptionCancellationEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.cancellationReason(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionBillingEvent createSubscriptionBillingEvent(NotificationContext context) {
        return new SubscriptionBillingEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.transactionId(),
            context.subscription().getBillingAmount(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionEvent createSubscriptionEventFromBatch(Subscription subscription, BatchNotificationContext context) {
        SubscriptionEventType eventType = switch (context.eventType()) {
            case "CREATED" -> SubscriptionEventType.SUBSCRIPTION_CREATED;
            case "ACTIVATED" -> SubscriptionEventType.SUBSCRIPTION_ACTIVATED;
            case "EXPIRED" -> SubscriptionEventType.SUBSCRIPTION_EXPIRED;
            default -> throw new IllegalArgumentException("Unknown batch event type: " + context.eventType());
        };
        
        return new SubscriptionEvent(
            subscription.getId(),
            subscription.getUserId(),
            eventType,
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    // Context Records for Functional Operations
    private record NotificationContext(
        String correlationId,
        Subscription subscription,
        String eventType,
        SubscriptionTier previousTier,
        String cancellationReason,
        UUID transactionId,
        LocalDateTime eventTime
    ) {}
    
    private record BatchNotificationContext(
        String correlationId,
        List<Subscription> subscriptions,
        String eventType
    ) {}
    
    // Event Records for Spring Events
    public record SubscriptionEvent(
        UUID subscriptionId,
        UUID userId,
        SubscriptionEventType eventType,
        LocalDateTime eventTime,
        String correlationId
    ) {}
    
    public record SubscriptionUpgradeEvent(
        UUID subscriptionId,
        UUID userId,
        SubscriptionTier previousTier,
        SubscriptionTier newTier,
        LocalDateTime eventTime,
        String correlationId
    ) {}
    
    public record SubscriptionCancellationEvent(
        UUID subscriptionId,
        UUID userId,
        String cancellationReason,
        LocalDateTime eventTime,
        String correlationId
    ) {}
    
    public record SubscriptionBillingEvent(
        UUID subscriptionId,
        UUID userId,
        UUID transactionId,
        java.math.BigDecimal billingAmount,
        LocalDateTime eventTime,
        String correlationId
    ) {}
    
    // Event Type Enum
    public enum SubscriptionEventType {
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_ACTIVATED,
        SUBSCRIPTION_UPGRADED,
        SUBSCRIPTION_CANCELLED,
        SUBSCRIPTION_BILLED,
        SUBSCRIPTION_EXPIRED,
        SUBSCRIPTION_SUSPENDED
    }
}