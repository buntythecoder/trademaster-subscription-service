package com.trademaster.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usage Tracking Entity
 * 
 * Tracks user feature usage against subscription limits.
 * Enables real-time enforcement of tier-based restrictions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "usage_tracking", 
    indexes = {
        @Index(name = "idx_usage_user_feature", columnList = "user_id, feature_name"),
        @Index(name = "idx_usage_subscription", columnList = "subscription_id"),
        @Index(name = "idx_usage_period", columnList = "period_start, period_end"),
        @Index(name = "idx_usage_reset_date", columnList = "reset_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_usage_user_feature_period", 
            columnNames = {"user_id", "feature_name", "period_start"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User whose usage is being tracked
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Associated subscription ID
     */
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    /**
     * Feature being tracked (watchlists, alerts, api_calls, etc.)
     */
    @Column(name = "feature_name", nullable = false, length = 50)
    private String featureName;
    
    /**
     * Feature name (alias for compatibility) - using 'feature' instead of 'featureName'
     */
    @Transient
    public String getFeature() {
        return this.featureName;
    }
    
    public void setFeature(String feature) {
        this.featureName = feature;
    }

    /**
     * Current usage count in this period
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    /**
     * Maximum allowed usage for this feature (-1 for unlimited)
     */
    @Column(name = "usage_limit", nullable = false)
    private Long usageLimit;

    /**
     * Period start date
     */
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    /**
     * Period end date
     */
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;
    
    /**
     * Billing period aliases for compatibility
     */
    @Transient
    public LocalDateTime getBillingPeriodStart() {
        return this.periodStart;
    }
    
    public void setBillingPeriodStart(LocalDateTime billingPeriodStart) {
        this.periodStart = billingPeriodStart;
    }
    
    @Transient
    public LocalDateTime getBillingPeriodEnd() {
        return this.periodEnd;
    }
    
    public void setBillingPeriodEnd(LocalDateTime billingPeriodEnd) {
        this.periodEnd = billingPeriodEnd;
    }

    /**
     * When usage counter resets (daily, monthly, etc.)
     */
    @Column(name = "reset_date", nullable = false)
    private LocalDateTime resetDate;
    
    /**
     * Last reset date alias for compatibility
     */
    @Transient
    public LocalDateTime getLastResetDate() {
        return this.resetDate;
    }
    
    public void setLastResetDate(LocalDateTime lastResetDate) {
        this.resetDate = lastResetDate;
    }
    
    /**
     * Last used date alias for compatibility
     */
    @Transient
    public LocalDateTime getLastUsedDate() {
        return this.getUpdatedAt();
    }
    
    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        // For now, we'll use updatedAt as proxy for lastUsedDate
        this.setUpdatedAt(lastUsedDate);
    }

    /**
     * Reset frequency in days (1 for daily, 30 for monthly, etc.)
     */
    @Column(name = "reset_frequency_days", nullable = false)
    @Builder.Default
    private Integer resetFrequencyDays = 30;

    /**
     * Flag to indicate if limit has been exceeded
     */
    @Column(name = "limit_exceeded")
    @Builder.Default
    private Boolean limitExceeded = false;

    /**
     * First time limit was exceeded in this period
     */
    @Column(name = "first_exceeded_at")
    private LocalDateTime firstExceededAt;

    /**
     * Number of times limit was exceeded in this period
     */
    @Column(name = "exceeded_count")
    @Builder.Default
    private Integer exceededCount = 0;

    /**
     * Additional metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Record creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}