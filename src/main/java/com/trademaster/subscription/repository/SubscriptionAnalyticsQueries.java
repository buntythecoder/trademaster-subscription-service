package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Subscription Analytics Queries
 * MANDATORY: Single Responsibility - Analytics and metrics queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Business intelligence and analytics query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionAnalyticsQueries {

    /**
     * Count active subscriptions by tier
     */
    @Query("SELECT s.tier, COUNT(s) FROM Subscription s WHERE s.status IN :activeStatuses GROUP BY s.tier")
    List<Object[]> countActiveSubscriptionsByTier(@Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Count subscriptions by status
     */
    @Query("SELECT s.status, COUNT(s) FROM Subscription s GROUP BY s.status")
    List<Object[]> countSubscriptionsByStatus();

    /**
     * Calculate monthly recurring revenue (MRR)
     */
    @Query("SELECT SUM(CASE s.billingCycle " +
           "WHEN 'MONTHLY' THEN s.billingAmount " +
           "WHEN 'QUARTERLY' THEN s.billingAmount / 3 " +
           "WHEN 'ANNUAL' THEN s.billingAmount / 12 " +
           "ELSE 0 END) " +
           "FROM Subscription s WHERE s.status IN :activeStatuses")
    Optional<Double> calculateMonthlyRecurringRevenue(@Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Calculate annual recurring revenue (ARR)
     */
    @Query("SELECT SUM(CASE s.billingCycle " +
           "WHEN 'MONTHLY' THEN s.billingAmount * 12 " +
           "WHEN 'QUARTERLY' THEN s.billingAmount * 4 " +
           "WHEN 'ANNUAL' THEN s.billingAmount " +
           "ELSE 0 END) " +
           "FROM Subscription s WHERE s.status IN :activeStatuses")
    Optional<Double> calculateAnnualRecurringRevenue(@Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find subscriptions created in date range
     */
    @Query("SELECT s FROM Subscription s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<Subscription> findCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find churned subscriptions in date range
     */
    @Query("SELECT s FROM Subscription s WHERE s.status IN ('CANCELLED', 'TERMINATED') " +
           "AND s.cancelledAt BETWEEN :startDate AND :endDate ORDER BY s.cancelledAt DESC")
    List<Subscription> findChurnedBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate churn rate for a period
     */
    @Query("SELECT " +
           "(SELECT COUNT(s) FROM Subscription s WHERE s.status IN ('CANCELLED', 'TERMINATED') " +
           " AND s.cancelledAt BETWEEN :startDate AND :endDate) * 1.0 / " +
           "(SELECT COUNT(s) FROM Subscription s WHERE s.status IN :activeStatuses " +
           " AND s.createdAt < :startDate) " +
           "FROM Subscription s WHERE s.id = s.id LIMIT 1")
    Optional<Double> calculateChurnRate(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find subscriptions for analytics dashboard with pagination
     */
    @Query("SELECT s FROM Subscription s WHERE " +
           "(:tier IS NULL OR s.tier = :tier) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:startDate IS NULL OR s.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR s.createdAt <= :endDate) " +
           "ORDER BY s.createdAt DESC")
    Page<Subscription> findForAnalytics(@Param("tier") SubscriptionTier tier,
                                       @Param("status") SubscriptionStatus status,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);
}
