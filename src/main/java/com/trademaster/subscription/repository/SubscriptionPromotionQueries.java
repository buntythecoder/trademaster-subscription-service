package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Subscription Promotion Queries
 * MANDATORY: Single Responsibility - Promotion-specific queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Promotion code subscription query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionPromotionQueries {

    /**
     * Find subscriptions with promotion codes
     */
    @Query("SELECT s FROM Subscription s WHERE s.promotionCode IS NOT NULL AND s.promotionCode != ''")
    List<Subscription> findWithPromotionCodes();

    /**
     * Find subscriptions by promotion code
     */
    List<Subscription> findByPromotionCode(String promotionCode);
}
