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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Entity
 * 
 * Core entity representing a user's subscription to TradeMaster services.
 * Manages subscription lifecycle, billing, and feature access.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_user_id", columnList = "user_id"),
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_tier", columnList = "tier"),
    @Index(name = "idx_subscription_billing_date", columnList = "next_billing_date"),
    @Index(name = "idx_subscription_active", columnList = "status, next_billing_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User who owns this subscription
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Current subscription tier
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private SubscriptionTier tier;

    /**
     * Current subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    /**
     * Billing cycle for this subscription
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    /**
     * Monthly price for this subscription (before discounts)
     */
    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    /**
     * Actual amount charged per billing cycle (after discounts)
     */
    @Column(name = "billing_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal billingAmount;

    /**
     * Currency code (INR, USD, etc.)
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    /**
     * When the subscription started
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * When the subscription ends (null for ongoing subscriptions)
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * Next billing date
     */
    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    /**
     * Last successful billing date
     */
    @Column(name = "last_billing_date")
    private LocalDateTime lastBillingDate;

    /**
     * Trial end date (if applicable)
     */
    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    /**
     * Number of failed billing attempts
     */
    @Column(name = "failed_billing_attempts")
    @Builder.Default
    private Integer failedBillingAttempts = 0;

    /**
     * Auto-renewal enabled flag
     */
    @Column(name = "auto_renewal")
    @Builder.Default
    private Boolean autoRenewal = true;

    /**
     * Cancellation reason (if cancelled)
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Date when subscription was cancelled
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Payment method ID from payment service
     */
    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    /**
     * Customer ID from payment gateway
     */
    @Column(name = "gateway_customer_id", length = 100)
    private String gatewayCustomerId;

    /**
     * Subscription ID from payment gateway
     */
    @Column(name = "gateway_subscription_id", length = 100)
    private String gatewaySubscriptionId;

    /**
     * Promotional discount applied
     */
    @Column(name = "promotion_discount", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    /**
     * Promotion code used
     */
    @Column(name = "promotion_code", length = 50)
    private String promotionCode;
    
    /**
     * When subscription was activated (if applicable)
     */
    @Column(name = "activated_date")
    private LocalDateTime activatedDate;
    
    /**
     * When subscription was cancelled (if applicable)
     */
    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;
    
    /**
     * When subscription was upgraded (if applicable)
     */
    @Column(name = "upgraded_date")
    private LocalDateTime upgradedDate;
    
    /**
     * Last billing date
     */
    @Column(name = "last_billed_date")
    private LocalDateTime lastBilledDate;

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

    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    // Business Logic Methods

    /**
     * Check if subscription is currently active
     */
    public boolean isActive() {
        return status.hasAccess() && 
               (endDate == null || endDate.isAfter(LocalDateTime.now()));
    }

    /**
     * Check if subscription is in trial period
     */
    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL &&
               trialEndDate != null &&
               trialEndDate.isAfter(LocalDateTime.now());
    }

    /**
     * Check if subscription has expired
     */
    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    /**
     * Check if subscription can be billed
     */
    public boolean canBeBilled() {
        return status.isBillable() && autoRenewal && nextBillingDate != null;
    }

    /**
     * Check if subscription is due for billing
     */
    public boolean isDueForBilling() {
        return canBeBilled() && nextBillingDate.isBefore(LocalDateTime.now());
    }

    /**
     * Check if subscription is in grace period
     */
    public boolean isInGracePeriod() {
        return status == SubscriptionStatus.EXPIRED &&
               nextBillingDate != null &&
               nextBillingDate.plusDays(3).isAfter(LocalDateTime.now());
    }

    /**
     * Calculate days remaining in current billing cycle
     */
    public long getDaysRemainingInCycle() {
        if (nextBillingDate == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), nextBillingDate).toDays();
    }

    /**
     * Calculate monthly savings compared to monthly billing
     */
    public BigDecimal getMonthlySavings() {
        if (billingCycle == BillingCycle.MONTHLY) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal monthlyTotal = monthlyPrice;
        BigDecimal effectiveMonthly = billingAmount.divide(
            BigDecimal.valueOf(billingCycle.getMonths()), 
            2, 
            java.math.RoundingMode.HALF_UP
        );
        
        return monthlyTotal.subtract(effectiveMonthly);
    }

    /**
     * Activate subscription
     */
    public void activate() {
        if (status == SubscriptionStatus.TRIAL) {
            this.status = SubscriptionStatus.ACTIVE;
        } else if (status.canTransitionTo(SubscriptionStatus.ACTIVE)) {
            this.status = SubscriptionStatus.ACTIVE;
            this.startDate = LocalDateTime.now();
            this.failedBillingAttempts = 0;
        }
    }

    /**
     * Cancel subscription
     */
    public void cancel(String reason) {
        if (status.canCancel()) {
            this.status = SubscriptionStatus.CANCELLED;
            this.cancellationReason = reason;
            this.cancelledAt = LocalDateTime.now();
            this.autoRenewal = false;
        }
    }

    /**
     * Suspend subscription
     */
    public void suspend() {
        if (status.canTransitionTo(SubscriptionStatus.SUSPENDED)) {
            this.status = SubscriptionStatus.SUSPENDED;
        }
    }

    /**
     * Update next billing date based on billing cycle
     */
    public void updateNextBillingDate() {
        if (nextBillingDate != null) {
            this.nextBillingDate = billingCycle.getNextBillingDate(nextBillingDate);
        } else if (startDate != null) {
            this.nextBillingDate = billingCycle.getNextBillingDate(startDate);
        }
    }

    /**
     * Record successful billing
     */
    public void recordSuccessfulBilling() {
        this.lastBillingDate = LocalDateTime.now();
        this.failedBillingAttempts = 0;
        updateNextBillingDate();
        
        if (status == SubscriptionStatus.SUSPENDED || status == SubscriptionStatus.EXPIRED) {
            this.status = SubscriptionStatus.ACTIVE;
        }
    }

    /**
     * Record failed billing attempt
     */
    public void recordFailedBilling() {
        this.failedBillingAttempts++;
        
        if (failedBillingAttempts >= 3) {
            this.status = SubscriptionStatus.SUSPENDED;
        }
    }
}