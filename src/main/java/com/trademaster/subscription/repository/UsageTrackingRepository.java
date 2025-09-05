package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Usage Tracking Repository
 * 
 * Data access layer for usage tracking and limit enforcement.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, UUID> {

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
     * Bulk increment usage
     */
    @Modifying
    @Query("UPDATE UsageTracking ut SET ut.usageCount = ut.usageCount + :amount, " +
           "ut.updatedAt = :updateTime " +
           "WHERE ut.userId = :userId AND ut.featureName = :featureName " +
           "AND ut.periodStart <= :currentDate AND ut.periodEnd > :currentDate")
    int incrementUsage(@Param("userId") UUID userId,
                      @Param("featureName") String featureName,
                      @Param("amount") Long amount,
                      @Param("currentDate") LocalDateTime currentDate,
                      @Param("updateTime") LocalDateTime updateTime);

    /**
     * Reset usage for new period
     */
    @Modifying
    @Query("UPDATE UsageTracking ut SET ut.usageCount = 0, ut.limitExceeded = false, " +
           "ut.exceededCount = 0, ut.firstExceededAt = null, " +
           "ut.periodStart = :newPeriodStart, ut.periodEnd = :newPeriodEnd, " +
           "ut.resetDate = :newResetDate, ut.updatedAt = :updateTime " +
           "WHERE ut.id IN :usageIds")
    int resetUsageBulk(@Param("usageIds") List<UUID> usageIds,
                      @Param("newPeriodStart") LocalDateTime newPeriodStart,
                      @Param("newPeriodEnd") LocalDateTime newPeriodEnd,
                      @Param("newResetDate") LocalDateTime newResetDate,
                      @Param("updateTime") LocalDateTime updateTime);

    /**
     * Update usage limits when subscription changes
     */
    @Modifying
    @Query("UPDATE UsageTracking ut SET ut.usageLimit = :newLimit, " +
           "ut.limitExceeded = CASE WHEN :newLimit = -1 THEN false " +
           "                        WHEN ut.usageCount > :newLimit THEN true " +
           "                        ELSE false END, " +
           "ut.updatedAt = :updateTime " +
           "WHERE ut.subscriptionId = :subscriptionId")
    int updateUsageLimits(@Param("subscriptionId") UUID subscriptionId,
                         @Param("newLimit") Long newLimit,
                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * Delete old usage records for cleanup
     */
    @Modifying
    @Query("DELETE FROM UsageTracking ut WHERE ut.periodEnd < :cutoffDate")
    int deleteOldUsageRecords(@Param("cutoffDate") LocalDateTime cutoffDate);

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