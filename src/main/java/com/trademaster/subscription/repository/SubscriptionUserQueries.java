package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Subscription User Queries
 * MANDATORY: Single Responsibility - User-specific queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * User-related subscription query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionUserQueries {

    /**
     * Find active subscription by user ID
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status IN :activeStatuses ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveByUserId(@Param("userId") UUID userId,
                                            @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find current subscription by user ID (most recent)
     */
    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all subscriptions by user ID
     */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find subscriptions by user ID (simple version)
     */
    List<Subscription> findByUserId(UUID userId);

    /**
     * Check if user has any active subscription
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.userId = :userId AND s.status IN :activeStatuses")
    boolean hasActiveSubscription(@Param("userId") UUID userId,
                                 @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);
}
