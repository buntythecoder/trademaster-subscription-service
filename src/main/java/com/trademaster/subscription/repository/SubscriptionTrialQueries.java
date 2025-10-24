package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Subscription Trial Queries
 * MANDATORY: Single Responsibility - Trial-specific queries only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Trial period subscription query methods.
 *
 * @author TradeMaster Development Team
 */
public interface SubscriptionTrialQueries {

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
}
