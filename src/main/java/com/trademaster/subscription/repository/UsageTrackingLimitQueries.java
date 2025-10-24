package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Usage Tracking Limit Queries
 * MANDATORY: Single Responsibility - Limit monitoring and enforcement only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Usage limit monitoring and enforcement query methods.
 *
 * @author TradeMaster Development Team
 */
public interface UsageTrackingLimitQueries {

    /**
     * Find users exceeding limits
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.limitExceeded = true " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    List<UsageTracking> findUsersExceedingLimits(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find usage records needing reset
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.resetDate <= :currentDate")
    List<UsageTracking> findUsageNeedingReset(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find users approaching limits (>80% usage)
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.usageLimit > 0 " +
           "AND (ut.usageCount * 1.0 / ut.usageLimit) > 0.8 " +
           "AND ut.limitExceeded = false " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    List<UsageTracking> findUsersApproachingLimits(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find users at soft limit (>90% usage)
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.usageLimit > 0 " +
           "AND (ut.usageCount * 1.0 / ut.usageLimit) > 0.9 " +
           "AND ut.limitExceeded = false " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    List<UsageTracking> findUsersAtSoftLimit(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Check if user can use feature (within limits)
     */
    @Query("SELECT CASE " +
           "WHEN ut.usageLimit = -1 THEN true " +
           "WHEN ut.usageCount < ut.usageLimit THEN true " +
           "ELSE false END " +
           "FROM UsageTracking ut WHERE ut.userId = :userId AND ut.featureName = :featureName " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    Optional<Boolean> canUseFeature(@Param("userId") UUID userId,
                                   @Param("featureName") String featureName,
                                   @Param("currentDate") LocalDateTime currentDate);
}
