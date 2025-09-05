package com.trademaster.subscription.service.interfaces;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionTier;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Notification Service Interface
 * 
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * MANDATORY: Dependency Inversion - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public interface ISubscriptionNotificationService {
    
    /**
     * Publish subscription created event
     */
    CompletableFuture<Result<Void, String>> publishSubscriptionCreated(Subscription subscription);
    
    /**
     * Publish subscription activated event
     */
    CompletableFuture<Result<Void, String>> publishSubscriptionActivated(Subscription subscription);
    
    /**
     * Publish subscription upgraded event
     */
    CompletableFuture<Result<Void, String>> publishSubscriptionUpgraded(
        Subscription subscription, 
        SubscriptionTier previousTier
    );
    
    /**
     * Publish subscription cancelled event
     */
    CompletableFuture<Result<Void, String>> publishSubscriptionCancelled(
        Subscription subscription, 
        String cancellationReason
    );
    
    /**
     * Publish subscription billing event
     */
    CompletableFuture<Result<Void, String>> publishSubscriptionBilled(
        Subscription subscription, 
        UUID transactionId
    );
    
    /**
     * Publish batch notifications for multiple subscriptions
     */
    CompletableFuture<Result<List<UUID>, String>> publishBatchNotifications(
        List<Subscription> subscriptions, 
        String eventType
    );
}