package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.service.interfaces.ISubscriptionNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Notification Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized services:
 * - SubscriptionEventPublisher: Individual event publishing
 * - BatchNotificationProcessor: Batch notification processing
 *
 * @author TradeMaster Development Team
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionNotificationService implements ISubscriptionNotificationService {

    private final SubscriptionEventPublisher eventPublisher;
    private final BatchNotificationProcessor batchProcessor;

    /**
     * Publish subscription created event - Delegates to SubscriptionEventPublisher
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCreated(
            Subscription subscription) {
        return eventPublisher.publishCreated(subscription);
    }

    /**
     * Publish subscription activated event - Delegates to SubscriptionEventPublisher
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionActivated(
            Subscription subscription) {
        return eventPublisher.publishActivated(subscription);
    }

    /**
     * Publish subscription upgraded event - Delegates to SubscriptionEventPublisher
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionUpgraded(
            Subscription subscription, SubscriptionTier previousTier) {
        return eventPublisher.publishUpgraded(subscription, previousTier);
    }

    /**
     * Publish subscription cancelled event - Delegates to SubscriptionEventPublisher
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCancelled(
            Subscription subscription, String cancellationReason) {
        return eventPublisher.publishCancelled(subscription, cancellationReason);
    }

    /**
     * Publish subscription billing event - Delegates to SubscriptionEventPublisher
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionBilled(
            Subscription subscription, UUID transactionId) {
        return eventPublisher.publishBilled(subscription, transactionId);
    }

    /**
     * Publish batch notifications - Delegates to BatchNotificationProcessor
     */
    public CompletableFuture<Result<List<UUID>, String>> publishBatchNotifications(
            List<Subscription> subscriptions, String eventType) {
        return batchProcessor.processBatchNotifications(subscriptions, eventType);
    }
}
