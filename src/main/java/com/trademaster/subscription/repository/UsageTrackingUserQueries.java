package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Usage Tracking User Queries
 * MANDATORY: Single Responsibility - User/subscription specific queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * User and subscription-related usage tracking query methods.
 *
 * @author TradeMaster Development Team
 */
public interface UsageTrackingUserQueries {

    /**
     * Find current usage for user and feature
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.userId = :userId AND ut.featureName = :featureName " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    Optional<UsageTracking> findCurrentUsage(@Param("userId") UUID userId,
                                           @Param("featureName") String featureName,
                                           @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find all current usage for user
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.userId = :userId " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    List<UsageTracking> findCurrentUsageByUser(@Param("userId") UUID userId,
                                              @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find usage by subscription
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.subscriptionId = :subscriptionId " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    List<UsageTracking> findCurrentUsageBySubscription(@Param("subscriptionId") UUID subscriptionId,
                                                      @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find usage by subscription ID and feature name
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.subscriptionId = :subscriptionId AND ut.feature = :featureName " +
           "AND ut.billingPeriodStart <= :currentDate AND ut.billingPeriodEnd > :currentDate")
    Optional<UsageTracking> findBySubscriptionIdAndFeature(@Param("subscriptionId") UUID subscriptionId,
                                                          @Param("featureName") String featureName,
                                                          @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find usage by subscription ID and feature name (simplified without date check)
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.subscriptionId = :subscriptionId AND ut.feature = :featureName")
    Optional<UsageTracking> findBySubscriptionIdAndFeature(@Param("subscriptionId") UUID subscriptionId,
                                                          @Param("featureName") String featureName);

    /**
     * Find all usage records by subscription ID
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.subscriptionId = :subscriptionId")
    List<UsageTracking> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    /**
     * Find all usage records by user ID
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.userId = :userId")
    List<UsageTracking> findByUserId(@Param("userId") UUID userId);
}
