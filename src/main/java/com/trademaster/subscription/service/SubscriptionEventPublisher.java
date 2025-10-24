package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.constants.TemplateNameConstants;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.service.base.BaseNotificationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Event Publisher
 * MANDATORY: Single Responsibility - Handles individual event publishing only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SubscriptionEventPublisher extends BaseNotificationService {

    public SubscriptionEventPublisher(ApplicationEventPublisher eventPublisher,
                                     SubscriptionMetricsService metricsService,
                                     StructuredLoggingService loggingService,
                                     CircuitBreaker notificationCircuitBreaker,
                                     Retry notificationRetry) {
        super(eventPublisher, metricsService, loggingService, notificationCircuitBreaker, notificationRetry);
    }

    public CompletableFuture<Result<Void, String>> publishCreated(Subscription sub) {
        return publishEvent(sub, "CREATED", TemplateNameConstants.SUBSCRIPTION_CREATED, null, null, null);
    }

    public CompletableFuture<Result<Void, String>> publishActivated(Subscription sub) {
        return publishEvent(sub, "ACTIVATED", TemplateNameConstants.SUBSCRIPTION_ACTIVATED, null, null, null);
    }

    public CompletableFuture<Result<Void, String>> publishUpgraded(Subscription sub, SubscriptionTier prev) {
        return publishEvent(sub, "UPGRADED", TemplateNameConstants.SUBSCRIPTION_UPGRADED, prev, null, null);
    }

    public CompletableFuture<Result<Void, String>> publishCancelled(Subscription sub, String reason) {
        return publishEvent(sub, "CANCELLED", TemplateNameConstants.SUBSCRIPTION_CANCELLED, null, reason, null);
    }

    public CompletableFuture<Result<Void, String>> publishBilled(Subscription sub, UUID txId) {
        return publishEvent(sub, "BILLED", TemplateNameConstants.BILLING_SUCCESS, null, null, txId);
    }

    private CompletableFuture<Result<Void, String>> publishEvent(
            Subscription sub, String type, String metric, SubscriptionTier prev, String reason, UUID txId) {
        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return createContext(sub, type, corrId, prev, reason, txId)
                .flatMap(this::publishToEventBus)
                .onSuccess(r -> {
                    metricsService.recordSubscriptionProcessingTime(timer, metric);
                    log.info("Published {} event: {}", type, sub.getId());
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, metric + "_failed");
                    log.error("Failed to publish {} event: {}", type, e);
                });
        }, getVirtualThreadExecutor());
    }

    private Result<Ctx, String> createContext(Subscription s, String type, String corrId,
                                              SubscriptionTier prev, String reason, UUID txId) {
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            return new Ctx(corrId, s, type, prev, reason, txId);
        }).mapError(ex -> "Failed to create context: " + ex.getMessage());
    }

    private Result<Void, String> publishToEventBus(Ctx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                eventPublisher.publishEvent(createEvent(ctx));
                return (Void) null;
            }).mapError(ex -> "Failed to publish: " + ex.getMessage())
        );
    }

    private Object createEvent(Ctx ctx) {
        LocalDateTime now = LocalDateTime.now();
        Subscription s = ctx.subscription();

        return switch (ctx.type()) {
            case "CREATED" -> new SubscriptionEvent(
                s.getId(), s.getUserId(), EventType.SUBSCRIPTION_CREATED, now, ctx.correlationId());
            case "ACTIVATED" -> new SubscriptionEvent(
                s.getId(), s.getUserId(), EventType.SUBSCRIPTION_ACTIVATED, now, ctx.correlationId());
            case "UPGRADED" -> new UpgradeEvent(
                s.getId(), s.getUserId(), ctx.previousTier(), s.getTier(), now, ctx.correlationId());
            case "CANCELLED" -> new CancellationEvent(
                s.getId(), s.getUserId(), ctx.cancellationReason(), now, ctx.correlationId());
            case "BILLED" -> new BillingEvent(
                s.getId(), s.getUserId(), ctx.transactionId(), s.getBillingAmount(), now, ctx.correlationId());
            default -> throw new IllegalArgumentException("Unknown type: " + ctx.type());
        };
    }

    private record Ctx(String correlationId, Subscription subscription, String type,
                      SubscriptionTier previousTier, String cancellationReason, UUID transactionId) {}

    public record SubscriptionEvent(UUID subscriptionId, UUID userId, EventType eventType,
                                   LocalDateTime eventTime, String correlationId) {}

    public record UpgradeEvent(UUID subscriptionId, UUID userId, SubscriptionTier previousTier,
                              SubscriptionTier newTier, LocalDateTime eventTime, String correlationId) {}

    public record CancellationEvent(UUID subscriptionId, UUID userId, String cancellationReason,
                                   LocalDateTime eventTime, String correlationId) {}

    public record BillingEvent(UUID subscriptionId, UUID userId, UUID transactionId,
                              BigDecimal billingAmount, LocalDateTime eventTime, String correlationId) {}

    public enum EventType {
        SUBSCRIPTION_CREATED, SUBSCRIPTION_ACTIVATED, SUBSCRIPTION_UPGRADED,
        SUBSCRIPTION_CANCELLED, SUBSCRIPTION_BILLED, SUBSCRIPTION_EXPIRED, SUBSCRIPTION_SUSPENDED
    }
}
