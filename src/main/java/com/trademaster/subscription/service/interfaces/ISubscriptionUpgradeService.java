package com.trademaster.subscription.service.interfaces;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionTier;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Upgrade Service Interface
 * 
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * MANDATORY: Dependency Inversion - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public interface ISubscriptionUpgradeService {
    
    /**
     * Upgrade subscription tier
     */
    CompletableFuture<Result<Subscription, String>> upgradeSubscription(
        UUID subscriptionId, 
        SubscriptionTier newTier
    );
}