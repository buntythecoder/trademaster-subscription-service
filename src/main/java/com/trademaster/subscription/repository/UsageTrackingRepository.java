package com.trademaster.subscription.repository;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Usage Tracking Repository
 * MANDATORY: Single Responsibility - Core repository with interface composition
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Main repository composing all usage tracking query interfaces using
 * Spring Data JPA interface composition pattern.
 *
 * Query interfaces are organized by responsibility:
 * - UsageTrackingUserQueries: User and subscription queries
 * - UsageTrackingLimitQueries: Limit monitoring and enforcement
 * - UsageTrackingAnalyticsQueries: Statistics and analytics
 * - UsageTrackingBulkOperations: Bulk updates and maintenance
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Repository
public interface UsageTrackingRepository extends
        JpaRepository<UsageTracking, UUID>,
        UsageTrackingUserQueries,
        UsageTrackingLimitQueries,
        UsageTrackingAnalyticsQueries,
        UsageTrackingBulkOperations {
    // All query methods inherited from composed interfaces
}
