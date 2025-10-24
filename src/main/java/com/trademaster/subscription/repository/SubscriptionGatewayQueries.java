package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;

import java.util.Optional;

/**
 * Subscription Gateway Queries
 * MANDATORY: Single Responsibility - Gateway integration queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Payment gateway integration query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionGatewayQueries {

    /**
     * Find subscriptions by gateway customer ID
     */
    Optional<Subscription> findByGatewayCustomerId(String gatewayCustomerId);

    /**
     * Find subscriptions by gateway subscription ID
     */
    Optional<Subscription> findByGatewaySubscriptionId(String gatewaySubscriptionId);
}
