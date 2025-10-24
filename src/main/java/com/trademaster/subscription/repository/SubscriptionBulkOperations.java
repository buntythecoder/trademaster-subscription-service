package com.trademaster.subscription.repository;

import com.trademaster.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Subscription Bulk Operations
 * MANDATORY: Single Responsibility - Bulk update operations only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Bulk modification operations for subscriptions.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionBulkOperations {

    /**
     * Update subscription status in bulk
     */
    @Modifying
    @Query("UPDATE Subscription s SET s.status = :newStatus, s.updatedAt = :updateTime " +
           "WHERE s.id IN :subscriptionIds")
    int updateStatusBulk(@Param("subscriptionIds") List<UUID> subscriptionIds,
                        @Param("newStatus") SubscriptionStatus newStatus,
                        @Param("updateTime") LocalDateTime updateTime);
}
