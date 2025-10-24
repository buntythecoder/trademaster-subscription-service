package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Subscription High-Value Queries
 * MANDATORY: Single Responsibility - High-value subscription queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * High-value and premium subscription query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionHighValueQueries {

    /**
     * Find high-value subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.billingAmount >= :minAmount AND s.status IN :activeStatuses " +
           "ORDER BY s.billingAmount DESC")
    List<Subscription> findHighValueSubscriptions(@Param("minAmount") Double minAmount,
                                                @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find subscriptions with auto-renewal disabled
     */
    @Query("SELECT s FROM Subscription s WHERE s.autoRenewal = false AND s.status IN :activeStatuses")
    List<Subscription> findWithAutoRenewalDisabled(@Param("activeStatuses") List<SubscriptionStatus> activeStatuses);
}
