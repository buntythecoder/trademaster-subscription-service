package com.trademaster.subscription.entity;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionHistoryChangeType;
import com.trademaster.subscription.enums.SubscriptionHistoryInitiatedBy;
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
 * MANDATORY: Single Responsibility - Subscription audit trail only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Maintains audit trail of all subscription changes for compliance,
 * analytics, and customer support purposes.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
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
    private SubscriptionHistoryChangeType changeType;

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
    private SubscriptionHistoryInitiatedBy initiatedBy;

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
}