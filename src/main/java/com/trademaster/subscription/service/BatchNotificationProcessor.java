package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.events.SubscriptionEventType;
import com.trademaster.subscription.events.SubscriptionNotificationEvent;
import com.trademaster.subscription.service.base.BaseNotificationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Batch Notification Processor
 * MANDATORY: Single Responsibility - Handles batch notification processing only
 * MANDATORY: Functional Programming - Railway pattern with Stream API
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class BatchNotificationProcessor extends BaseNotificationService {

    public BatchNotificationProcessor(ApplicationEventPublisher eventPublisher,
                                     SubscriptionMetricsService metricsService,
                                     StructuredLoggingService loggingService,
                                     CircuitBreaker notificationCircuitBreaker,
                                     Retry notificationRetry) {
        super(eventPublisher, metricsService, loggingService, notificationCircuitBreaker, notificationRetry);
    }

    public CompletableFuture<Result<List<UUID>, String>> processBatchNotifications(
            List<Subscription> subscriptions, String eventType) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return createBatchContext(subscriptions, eventType, corrId)
                .flatMap(this::processBatch)
                .onSuccess(ids -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "batch_notifications");
                    log.info("Processed batch notifications: {} subscriptions", subscriptions.size());
                })
                .onFailure(error -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "batch_notifications_failed");
                    log.error("Failed to process batch: {}", error);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<BatchContext, String> createBatchContext(
            List<Subscription> subscriptions, String eventType, String corrId) {

        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            return new BatchContext(corrId, subscriptions, eventType);
        }).mapError(ex -> "Failed to create batch context: " + ex.getMessage());
    }

    private Result<List<UUID>, String> processBatch(BatchContext ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                List<UUID> processedIds = ctx.subscriptions().stream()
                    .map(subscription -> publishBatchEvent(subscription, ctx))
                    .toList();

                return processedIds;
            }).mapError(ex -> "Failed to process batch: " + ex.getMessage())
        );
    }

    private UUID publishBatchEvent(Subscription subscription, BatchContext ctx) {
        SubscriptionEventType eventType = switch (ctx.eventType()) {
            case "CREATED" -> SubscriptionEventType.SUBSCRIPTION_CREATED;
            case "ACTIVATED" -> SubscriptionEventType.SUBSCRIPTION_ACTIVATED;
            case "EXPIRED" -> SubscriptionEventType.SUBSCRIPTION_EXPIRED;
            default -> throw new IllegalArgumentException("Unknown batch event type: " + ctx.eventType());
        };

        eventPublisher.publishEvent(new SubscriptionNotificationEvent(
            subscription.getId(), subscription.getUserId(),
            eventType, LocalDateTime.now(), ctx.correlationId()));

        return subscription.getId();
    }

    private record BatchContext(String correlationId, List<Subscription> subscriptions,
                               String eventType) {}
}
