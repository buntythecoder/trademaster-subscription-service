package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Subscription Repository
 * 
 * Data access layer for subscription management with optimized queries
 * for performance and business intelligence.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

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
     * Find subscriptions by status
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find subscriptions by tier
     */
    List<Subscription> findByTier(SubscriptionTier tier);

    /**
     * Find subscriptions due for billing
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate <= :cutoffDate AND s.autoRenewal = true " +
           "AND s.status IN ('ACTIVE', 'EXPIRED') ORDER BY s.nextBillingDate ASC")
    List<Subscription> findDueForBilling(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find subscriptions in trial period
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate > :currentDate")
    List<Subscription> findActiveTrials(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find trials ending soon
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate BETWEEN :currentDate AND :endDate")
    List<Subscription> findTrialsEndingSoon(@Param("currentDate") LocalDateTime currentDate, 
                                          @Param("endDate") LocalDateTime endDate);

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
     * Find subscriptions by gateway customer ID
     */
    Optional<Subscription> findByGatewayCustomerId(String gatewayCustomerId);

    /**
     * Find subscriptions by gateway subscription ID
     */
    Optional<Subscription> findByGatewaySubscriptionId(String gatewaySubscriptionId);

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
     * Find subscriptions with promotion codes
     */
    @Query("SELECT s FROM Subscription s WHERE s.promotionCode IS NOT NULL AND s.promotionCode != ''")
    List<Subscription> findWithPromotionCodes();

    /**
     * Find subscriptions by promotion code
     */
    List<Subscription> findByPromotionCode(String promotionCode);
    
    /**
     * Find subscriptions due for billing
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate <= :currentDate AND s.status IN ('ACTIVE', 'TRIAL')")
    List<Subscription> findSubscriptionsDueForBilling(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Find high-value subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.billingAmount >= :minAmount AND s.status IN :activeStatuses " +
           "ORDER BY s.billingAmount DESC")
    List<Subscription> findHighValueSubscriptions(@Param("minAmount") Double minAmount,
                                                @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find subscriptions with auto-renewal disabled
     */
    @Query("SELECT s FROM Subscription s WHERE s.autoRenewal = false AND s.status IN :activeStatuses")
    List<Subscription> findWithAutoRenewalDisabled(@Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Update subscription status in bulk
     */
    @Modifying
    @Query("UPDATE Subscription s SET s.status = :newStatus, s.updatedAt = :updateTime " +
           "WHERE s.id IN :subscriptionIds")
    int updateStatusBulk(@Param("subscriptionIds") List<UUID> subscriptionIds, 
                        @Param("newStatus") SubscriptionStatus newStatus,
                        @Param("updateTime") LocalDateTime updateTime);

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

    /**
     * Check if user has any active subscription
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.userId = :userId AND s.status IN :activeStatuses")
    boolean hasActiveSubscription(@Param("userId") UUID userId, 
                                 @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);

    /**
     * Find upcoming renewals (next 7 days)
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate BETWEEN :startDate AND :endDate " +
           "AND s.autoRenewal = true AND s.status IN :activeStatuses ORDER BY s.nextBillingDate ASC")
    List<Subscription> findUpcomingRenewals(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("activeStatuses") List<SubscriptionStatus> activeStatuses);
}