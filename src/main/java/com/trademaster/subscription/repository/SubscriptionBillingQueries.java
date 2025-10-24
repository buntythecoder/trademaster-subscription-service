package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Subscription Billing Queries
 * MANDATORY: Single Responsibility - Billing-specific queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Billing-related subscription query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionBillingQueries {

    /**
     * Find subscriptions due for billing
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate <= :cutoffDate AND s.autoRenewal = true " +
           "AND s.status IN ('ACTIVE', 'EXPIRED') ORDER BY s.nextBillingDate ASC")
    List<Subscription> findDueForBilling(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find subscriptions due for billing (alternative query)
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate <= :currentDate AND s.status IN ('ACTIVE', 'TRIAL')")
    List<Subscription> findSubscriptionsDueForBilling(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find expired subscriptions within grace period
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'EXPIRED' AND s.nextBillingDate > :gracePeriodStart")
    List<Subscription> findInGracePeriod(@Param("gracePeriodStart") LocalDateTime gracePeriodStart);

    /**
     * Find suspended subscriptions with failed billing attempts
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'SUSPENDED' AND s.failedBillingAttempts >= :minAttempts")
    List<Subscription> findSuspendedWithFailedBilling(@Param("minAttempts") Integer minAttempts);

    /**
     * Find upcoming renewals (next 7 days)
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate BETWEEN :startDate AND :endDate " +
           "AND s.autoRenewal = true AND s.status IN :activeStatuses ORDER BY s.nextBillingDate ASC")
    List<Subscription> findUpcomingRenewals(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("activeStatuses") List<com.trademaster.subscription.enums.SubscriptionStatus> activeStatuses);

    /**
     * Update next billing date for subscriptions
     */
    @Modifying
    @Query("UPDATE Subscription s SET s.nextBillingDate = :nextBillingDate, s.updatedAt = :updateTime " +
           "WHERE s.id = :subscriptionId")
    int updateNextBillingDate(@Param("subscriptionId") UUID subscriptionId,
                             @Param("nextBillingDate") LocalDateTime nextBillingDate,
                             @Param("updateTime") LocalDateTime updateTime);

    /**
     * Reset failed billing attempts
     */
    @Modifying
    @Query("UPDATE Subscription s SET s.failedBillingAttempts = 0, s.updatedAt = :updateTime " +
           "WHERE s.id IN :subscriptionIds")
    int resetFailedBillingAttempts(@Param("subscriptionIds") List<UUID> subscriptionIds,
                                  @Param("updateTime") LocalDateTime updateTime);
}
