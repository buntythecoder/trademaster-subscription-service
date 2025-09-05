package com.trademaster.subscription.entity;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription History Entity
 * 
 * Maintains audit trail of all subscription changes for compliance,
 * analytics, and customer support purposes.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "subscription_history", indexes = {
    @Index(name = "idx_history_subscription_id", columnList = "subscription_id"),
    @Index(name = "idx_history_user_id", columnList = "user_id"),
    @Index(name = "idx_history_change_type", columnList = "change_type"),
    @Index(name = "idx_history_created_at", columnList = "created_at"),
    @Index(name = "idx_history_subscription_date", columnList = "subscription_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to the subscription
     */
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    /**
     * User who owns the subscription
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Type of change that occurred
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    /**
     * Previous subscription tier
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_tier")
    private SubscriptionTier oldTier;

    /**
     * New subscription tier
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_tier")
    private SubscriptionTier newTier;

    /**
     * Previous subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private SubscriptionStatus oldStatus;

    /**
     * New subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private SubscriptionStatus newStatus;

    /**
     * Previous billing cycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_billing_cycle")
    private BillingCycle oldBillingCycle;

    /**
     * New billing cycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_billing_cycle")
    private BillingCycle newBillingCycle;

    /**
     * Previous monthly price
     */
    @Column(name = "old_monthly_price", precision = 10, scale = 2)
    private BigDecimal oldMonthlyPrice;

    /**
     * New monthly price
     */
    @Column(name = "new_monthly_price", precision = 10, scale = 2)
    private BigDecimal newMonthlyPrice;

    /**
     * Previous billing amount
     */
    @Column(name = "old_billing_amount", precision = 10, scale = 2)
    private BigDecimal oldBillingAmount;

    /**
     * New billing amount
     */
    @Column(name = "new_billing_amount", precision = 10, scale = 2)
    private BigDecimal newBillingAmount;

    /**
     * Reason for the change
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * Who initiated the change (USER, SYSTEM, ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false)
    private InitiatedBy initiatedBy;

    /**
     * ID of the user/admin who made the change
     */
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    /**
     * Payment transaction ID if related to billing
     */
    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    /**
     * Action performed (CREATED, ACTIVATED, CANCELLED, UPGRADED, etc.)
     */
    @Column(name = "action", length = 50)
    private String action;
    
    /**
     * Additional metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Effective date of the change
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;

    /**
     * Record creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Change types for subscription history
     */
    public enum ChangeType {
        CREATED("Subscription Created"),
        ACTIVATED("Subscription Activated"),
        UPGRADED("Tier Upgraded"),
        DOWNGRADED("Tier Downgraded"),
        BILLING_CYCLE_CHANGED("Billing Cycle Changed"),
        SUSPENDED("Subscription Suspended"),
        CANCELLED("Subscription Cancelled"),
        TERMINATED("Subscription Terminated"),
        REACTIVATED("Subscription Reactivated"),
        PAUSED("Subscription Paused"),
        RESUMED("Subscription Resumed"),
        TRIAL_STARTED("Trial Started"),
        TRIAL_ENDED("Trial Ended"),
        PAYMENT_FAILED("Payment Failed"),
        PAYMENT_SUCCEEDED("Payment Succeeded"),
        AUTO_RENEWAL_ENABLED("Auto-Renewal Enabled"),
        AUTO_RENEWAL_DISABLED("Auto-Renewal Disabled"),
        PRICE_CHANGED("Price Changed"),
        PROMOTION_APPLIED("Promotion Applied"),
        PROMOTION_REMOVED("Promotion Removed");

        private final String description;

        ChangeType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Who initiated the change
     */
    public enum InitiatedBy {
        USER("User Action"),
        SYSTEM("System Automated"),
        ADMIN("Administrator"),
        PAYMENT_GATEWAY("Payment Gateway"),
        SCHEDULED_TASK("Scheduled Task");

        private final String description;

        InitiatedBy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Business Logic Methods

    /**
     * Check if this was a tier upgrade
     */
    public boolean isUpgrade() {
        return changeType == ChangeType.UPGRADED ||
               (oldTier != null && newTier != null && oldTier.ordinal() < newTier.ordinal());
    }

    /**
     * Check if this was a tier downgrade
     */
    public boolean isDowngrade() {
        return changeType == ChangeType.DOWNGRADED ||
               (oldTier != null && newTier != null && oldTier.ordinal() > newTier.ordinal());
    }

    /**
     * Check if this was a billing cycle change
     */
    public boolean isBillingCycleChange() {
        return changeType == ChangeType.BILLING_CYCLE_CHANGED ||
               (oldBillingCycle != null && newBillingCycle != null && 
                oldBillingCycle != newBillingCycle);
    }

    /**
     * Check if this was a price change
     */
    public boolean isPriceChange() {
        return changeType == ChangeType.PRICE_CHANGED ||
               (oldBillingAmount != null && newBillingAmount != null &&
                oldBillingAmount.compareTo(newBillingAmount) != 0);
    }

    /**
     * Calculate revenue impact of this change
     */
    public BigDecimal getRevenueImpact() {
        if (newBillingAmount != null && oldBillingAmount != null) {
            return newBillingAmount.subtract(oldBillingAmount);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if this change affects billing
     */
    public boolean affectsBilling() {
        return changeType == ChangeType.UPGRADED ||
               changeType == ChangeType.DOWNGRADED ||
               changeType == ChangeType.BILLING_CYCLE_CHANGED ||
               changeType == ChangeType.PRICE_CHANGED ||
               changeType == ChangeType.PROMOTION_APPLIED ||
               changeType == ChangeType.PROMOTION_REMOVED;
    }

    /**
     * Check if this is a cancellation event
     */
    public boolean isCancellation() {
        return changeType == ChangeType.CANCELLED ||
               changeType == ChangeType.TERMINATED ||
               (newStatus != null && (newStatus == SubscriptionStatus.CANCELLED || 
                                    newStatus == SubscriptionStatus.TERMINATED));
    }

    /**
     * Check if this is a reactivation event
     */
    public boolean isReactivation() {
        return changeType == ChangeType.REACTIVATED ||
               changeType == ChangeType.RESUMED ||
               (oldStatus != null && newStatus != null &&
                !oldStatus.hasAccess() && newStatus.hasAccess());
    }

    /**
     * Get human-readable description of the change
     */
    public String getChangeDescription() {
        StringBuilder description = new StringBuilder();
        
        switch (changeType) {
            case UPGRADED:
                description.append("Upgraded from ")
                          .append(oldTier != null ? oldTier.getDisplayName() : "Unknown")
                          .append(" to ")
                          .append(newTier != null ? newTier.getDisplayName() : "Unknown");
                break;
            case DOWNGRADED:
                description.append("Downgraded from ")
                          .append(oldTier != null ? oldTier.getDisplayName() : "Unknown")
                          .append(" to ")
                          .append(newTier != null ? newTier.getDisplayName() : "Unknown");
                break;
            case BILLING_CYCLE_CHANGED:
                description.append("Changed billing cycle from ")
                          .append(oldBillingCycle != null ? oldBillingCycle.getDisplayName() : "Unknown")
                          .append(" to ")
                          .append(newBillingCycle != null ? newBillingCycle.getDisplayName() : "Unknown");
                break;
            default:
                description.append(changeType.getDescription());
        }
        
        if (changeReason != null && !changeReason.trim().isEmpty()) {
            description.append(" - ").append(changeReason);
        }
        
        return description.toString();
    }
}