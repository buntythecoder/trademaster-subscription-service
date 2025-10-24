package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usage Tracking Analytics Queries
 * MANDATORY: Single Responsibility - Analytics and statistics only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Usage analytics, statistics, and trend analysis query methods.
 *
 * @author TradeMaster Development Team
 */
public interface UsageTrackingAnalyticsQueries {

    /**
     * Get usage statistics for a feature
     */
    @Query("SELECT " +
           "COUNT(ut) as totalUsers, " +
           "AVG(ut.usageCount) as avgUsage, " +
           "MAX(ut.usageCount) as maxUsage, " +
           "COUNT(CASE WHEN ut.limitExceeded = true THEN 1 END) as exceededCount " +
           "FROM UsageTracking ut WHERE ut.featureName = :featureName " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    Object[] getFeatureUsageStatistics(@Param("featureName") String featureName,
                                      @Param("currentDate") LocalDateTime currentDate);

    /**
     * Get usage trend for user and feature
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.userId = :userId AND ut.featureName = :featureName " +
           "ORDER BY ut.periodStart DESC LIMIT :limit")
    List<UsageTracking> getUsageTrend(@Param("userId") UUID userId,
                                    @Param("featureName") String featureName,
                                    @Param("limit") int limit);

    /**
     * Find high-usage users for a feature
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.featureName = :featureName " +
           "AND ut.usageCount >= :minUsage " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate " +
           "ORDER BY ut.usageCount DESC")
    List<UsageTracking> findHighUsageUsers(@Param("featureName") String featureName,
                                         @Param("minUsage") Long minUsage,
                                         @Param("currentDate") LocalDateTime currentDate);

    /**
     * Count users by feature usage level
     */
    @Query("SELECT " +
           "CASE " +
           "WHEN ut.usageLimit = -1 THEN 'UNLIMITED' " +
           "WHEN ut.usageLimit = 0 THEN 'NO_LIMIT_SET' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 1.0 THEN 'EXCEEDED' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 0.9 THEN 'HIGH' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 0.6 THEN 'MEDIUM' " +
           "ELSE 'LOW' " +
           "END as usageLevel, " +
           "COUNT(ut) as userCount " +
           "FROM UsageTracking ut WHERE ut.featureName = :featureName " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate " +
           "GROUP BY " +
           "CASE " +
           "WHEN ut.usageLimit = -1 THEN 'UNLIMITED' " +
           "WHEN ut.usageLimit = 0 THEN 'NO_LIMIT_SET' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 1.0 THEN 'EXCEEDED' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 0.9 THEN 'HIGH' " +
           "WHEN (ut.usageCount * 1.0 / ut.usageLimit) >= 0.6 THEN 'MEDIUM' " +
           "ELSE 'LOW' " +
           "END")
    List<Object[]> countUsersByUsageLevel(@Param("featureName") String featureName,
                                         @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find usage records for export/analytics
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE " +
           "(:userId IS NULL OR ut.userId = :userId) AND " +
           "(:featureName IS NULL OR ut.featureName = :featureName) AND " +
           "(:startDate IS NULL OR ut.periodStart >= :startDate) AND " +
           "(:endDate IS NULL OR ut.periodEnd <= :endDate) " +
           "ORDER BY ut.periodStart DESC")
    List<UsageTracking> findForAnalytics(@Param("userId") UUID userId,
                                        @Param("featureName") String featureName,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
