package com.trademaster.subscription.service.interfaces;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Lifecycle Service Interface
 * 
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * MANDATORY: Dependency Inversion - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public interface ISubscriptionLifecycleService {
    
    /**
     * Create a new subscription
     */
    CompletableFuture<Result<Subscription, String>> createSubscription(
        UUID userId, 
        SubscriptionTier tier, 
        BillingCycle billingCycle, 
        boolean startTrial
    );
    
    /**
     * Activate subscription after payment
     */
    CompletableFuture<Result<Subscription, String>> activateSubscription(
        UUID subscriptionId, 
        UUID paymentTransactionId
    );
    
    /**
     * Cancel subscription
     */
    CompletableFuture<Result<Subscription, String>> cancelSubscription(
        UUID subscriptionId, 
        String cancellationReason
    );
    
    /**
     * Get active subscription for user
     */
    CompletableFuture<Result<Optional<Subscription>, String>> getActiveSubscription(UUID userId);
    
    /**
     * Find subscription by ID
     */
    CompletableFuture<Result<Optional<Subscription>, String>> findById(UUID subscriptionId);
    
    /**
     * Get user subscriptions with pagination
     */
    CompletableFuture<Result<Page<Subscription>, String>> getUserSubscriptions(UUID userId, Pageable pageable);
    
    /**
     * Suspend subscription
     */
    CompletableFuture<Result<Subscription, String>> suspendSubscription(UUID subscriptionId, String reason);
    
    /**
     * Get subscription history
     */
    CompletableFuture<Result<List<SubscriptionHistory>, String>> getSubscriptionHistory(UUID subscriptionId, Pageable pageable);
    
    /**
     * Check subscription health
     */
    CompletableFuture<Result<String, String>> checkSubscriptionHealth(UUID subscriptionId);
    
    /**
     * Get subscriptions by status
     */
    CompletableFuture<Result<Page<Subscription>, String>> getSubscriptionsByStatus(SubscriptionStatus status, Pageable pageable);
}