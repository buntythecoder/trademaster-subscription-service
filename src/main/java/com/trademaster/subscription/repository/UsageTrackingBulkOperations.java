package com.trademaster.subscription.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usage Tracking Bulk Operations
 * MANDATORY: Single Responsibility - Bulk updates and maintenance only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Bulk modification and maintenance operations for usage tracking.
 *
 * @author TradeMaster Development Team
 */
public interface UsageTrackingBulkOperations {

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
}
