package com.trademaster.subscription.service.interfaces;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.UsageTracking;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Usage Service Interface
 * 
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * MANDATORY: Dependency Inversion - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public interface ISubscriptionUsageService {
    
    /**
     * Check if feature usage is allowed for subscription
     */
    CompletableFuture<Result<Boolean, String>> canUseFeature(UUID subscriptionId, String featureName);
    
    /**
     * Increment usage for a feature
     */
    CompletableFuture<Result<UsageTracking, String>> incrementUsage(
        UUID subscriptionId, 
        String featureName, 
        int incrementBy
    );
    
    /**
     * Get current usage for subscription
     */
    CompletableFuture<Result<List<UsageTracking>, String>> getCurrentUsage(UUID subscriptionId);
    
    /**
     * Reset usage for billing period
     */
    CompletableFuture<Result<List<UsageTracking>, String>> resetUsageForBillingPeriod(UUID subscriptionId);
}