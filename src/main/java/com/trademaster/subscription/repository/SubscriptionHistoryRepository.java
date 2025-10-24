package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionHistoryChangeType;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Subscription History Repository
 * 
 * Data access layer for subscription audit trail and analytics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, UUID> {

    /**
     * Find history for a specific subscription
     */
    List<SubscriptionHistory> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    /**
     * Find history for a user
     */
    List<SubscriptionHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find history by change type
     */
    List<SubscriptionHistory> findByChangeTypeOrderByCreatedAtDesc(SubscriptionHistoryChangeType changeType);

    /**
     * Find history in date range
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Find upgrades in date range
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType = 'UPGRADED' " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findUpgrades(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find downgrades in date range
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType = 'DOWNGRADED' " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findDowngrades(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Find cancellations in date range
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType IN ('CANCELLED', 'TERMINATED') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findCancellations(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Find reactivations in date range
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType IN ('REACTIVATED', 'RESUMED') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findReactivations(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Count changes by type in date range
     */
    @Query("SELECT sh.changeType, COUNT(sh) FROM SubscriptionHistory sh " +
           "WHERE sh.createdAt BETWEEN :startDate AND :endDate GROUP BY sh.changeType")
    List<Object[]> countChangesByType(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Count tier movements (upgrades/downgrades)
     */
    @Query("SELECT " +
           "CONCAT(COALESCE(CAST(sh.oldTier AS string), 'NULL'), ' -> ', COALESCE(CAST(sh.newTier AS string), 'NULL')) as tierMovement, " +
           "COUNT(sh) as count " +
           "FROM SubscriptionHistory sh " +
           "WHERE sh.changeType IN ('UPGRADED', 'DOWNGRADED') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.oldTier, sh.newTier " +
           "ORDER BY count DESC")
    List<Object[]> countTierMovements(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate revenue impact of changes
     */
    @Query("SELECT " +
           "sh.changeType, " +
           "SUM(COALESCE(sh.newBillingAmount, 0) - COALESCE(sh.oldBillingAmount, 0)) as revenueImpact, " +
           "COUNT(sh) as changeCount " +
           "FROM SubscriptionHistory sh " +
           "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
           "AND (sh.oldBillingAmount IS NOT NULL OR sh.newBillingAmount IS NOT NULL) " +
           "GROUP BY sh.changeType " +
           "ORDER BY revenueImpact DESC")
    List<Object[]> calculateRevenueImpactByChangeType(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find most common cancellation reasons
     */
    @Query("SELECT sh.changeReason, COUNT(sh) as count FROM SubscriptionHistory sh " +
           "WHERE sh.changeType IN ('CANCELLED', 'TERMINATED') " +
           "AND sh.changeReason IS NOT NULL AND sh.changeReason != '' " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.changeReason ORDER BY count DESC")
    List<Object[]> findTopCancellationReasons(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Find users with frequent tier changes
     */
    @Query("SELECT sh.userId, COUNT(sh) as changeCount FROM SubscriptionHistory sh " +
           "WHERE sh.changeType IN ('UPGRADED', 'DOWNGRADED') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.userId HAVING COUNT(sh) >= :minChanges " +
           "ORDER BY changeCount DESC")
    List<Object[]> findUsersWithFrequentTierChanges(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("minChanges") Long minChanges);

    /**
     * Find payment-related changes
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType IN ('PAYMENT_FAILED', 'PAYMENT_SUCCEEDED') " +
           "AND sh.createdAt BETWEEN :startDate AND :endDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findPaymentChanges(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find changes initiated by system vs user
     */
    @Query("SELECT sh.initiatedBy, sh.changeType, COUNT(sh) FROM SubscriptionHistory sh " +
           "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sh.initiatedBy, sh.changeType " +
           "ORDER BY sh.initiatedBy, COUNT(sh) DESC")
    List<Object[]> countChangesByInitiator(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find subscription lifecycle for analytics
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.subscriptionId = :subscriptionId " +
           "AND sh.changeType IN ('CREATED', 'ACTIVATED', 'CANCELLED', 'TERMINATED') " +
           "ORDER BY sh.createdAt ASC")
    List<SubscriptionHistory> findSubscriptionLifecycle(@Param("subscriptionId") UUID subscriptionId);

    /**
     * Calculate average time between tier changes
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (next_change.created_at - sh.created_at))) as avg_seconds " +
           "FROM subscription_history sh " +
           "JOIN subscription_history next_change ON sh.subscription_id = next_change.subscription_id " +
           "WHERE sh.change_type IN ('UPGRADED', 'DOWNGRADED') " +
           "AND next_change.change_type IN ('UPGRADED', 'DOWNGRADED') " +
           "AND next_change.created_at > sh.created_at " +
           "AND sh.created_at >= :startDate AND sh.created_at <= :endDate", 
           nativeQuery = true)
    Optional<Double> calculateAvgTimeBetweenTierChanges(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find promotions usage history
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.changeType IN ('PROMOTION_APPLIED', 'PROMOTION_REMOVED') " +
           "ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findPromotionHistory();

    /**
     * Get change history with pagination for admin dashboard
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE " +
           "(:subscriptionId IS NULL OR sh.subscriptionId = :subscriptionId) AND " +
           "(:userId IS NULL OR sh.userId = :userId) AND " +
           "(:changeType IS NULL OR sh.changeType = :changeType) AND " +
           "(:startDate IS NULL OR sh.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR sh.createdAt <= :endDate) " +
           "ORDER BY sh.createdAt DESC")
    Page<SubscriptionHistory> findForDashboard(@Param("subscriptionId") UUID subscriptionId,
                                              @Param("userId") UUID userId,
                                              @Param("changeType") SubscriptionHistoryChangeType changeType,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    /**
     * Find recent changes for user notification
     */
    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.userId = :userId " +
           "AND sh.createdAt >= :sinceDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findRecentChangesForUser(@Param("userId") UUID userId,
                                                      @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count total changes by subscription
     */
    @Query("SELECT sh.subscriptionId, COUNT(sh) FROM SubscriptionHistory sh " +
           "GROUP BY sh.subscriptionId ORDER BY COUNT(sh) DESC")
    List<Object[]> countTotalChangesBySubscription();
}