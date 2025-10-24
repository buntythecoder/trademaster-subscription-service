package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Subscription Repository
 * MANDATORY: Single Responsibility - Core repository with interface composition
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Main repository composing all subscription query interfaces using
 * Spring Data JPA interface composition pattern.
 *
 * Query interfaces are organized by responsibility:
 * - SubscriptionUserQueries: User-specific queries
 * - SubscriptionBillingQueries: Billing and payment operations
 * - SubscriptionTrialQueries: Trial period management
 * - SubscriptionGatewayQueries: Payment gateway integration
 * - SubscriptionAnalyticsQueries: Business intelligence and metrics
 * - SubscriptionPromotionQueries: Promotion code handling
 * - SubscriptionHighValueQueries: High-value subscription management
 * - SubscriptionBulkOperations: Bulk update operations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Repository
public interface SubscriptionRepository extends
        JpaRepository<Subscription, UUID>,
        SubscriptionUserQueries,
        SubscriptionBillingQueries,
        SubscriptionTrialQueries,
        SubscriptionGatewayQueries,
        SubscriptionAnalyticsQueries,
        SubscriptionPromotionQueries,
        SubscriptionHighValueQueries,
        SubscriptionBulkOperations {

    /**
     * Find subscriptions by status
     * Basic query method using Spring Data JPA naming convention
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find subscriptions by tier
     * Basic query method using Spring Data JPA naming convention
     */
    List<Subscription> findByTier(SubscriptionTier tier);
}
