package com.trademaster.subscription.service.interfaces;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Billing Service Interface
 * 
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * MANDATORY: Dependency Inversion - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public interface ISubscriptionBillingService {
    
    /**
     * Process billing for subscription
     */
    CompletableFuture<Result<Subscription, String>> processBilling(
        UUID subscriptionId, 
        UUID paymentTransactionId
    );
    
    /**
     * Update billing cycle for subscription
     */
    CompletableFuture<Result<Subscription, String>> updateBillingCycle(
        UUID subscriptionId, 
        BillingCycle newBillingCycle
    );
    
    /**
     * Get upcoming billing amount for subscription
     */
    CompletableFuture<Result<BigDecimal, String>> getUpcomingBillingAmount(UUID subscriptionId);
    
    /**
     * Get all subscriptions due for billing
     */
    CompletableFuture<Result<List<Subscription>, String>> getSubscriptionsDueForBilling();
}