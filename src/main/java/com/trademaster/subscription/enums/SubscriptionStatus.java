package com.trademaster.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Subscription Status Enumeration
 * 
 * Defines all possible states of a subscription throughout its lifecycle.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionStatus {
    
    /**
     * Subscription is newly created but not yet activated
     */
    PENDING("Pending", "Subscription created but payment pending"),
    
    /**
     * Subscription is active and user has full access
     */
    ACTIVE("Active", "Subscription is active with full access"),
    
    /**
     * Trial period is active
     */
    TRIAL("Trial", "Free trial period is active"),
    
    /**
     * Subscription has expired but within grace period
     */
    EXPIRED("Expired", "Subscription expired but within grace period"),
    
    /**
     * Subscription is suspended due to payment failure
     */
    SUSPENDED("Suspended", "Subscription suspended due to payment issues"),
    
    /**
     * Payment processing has failed
     */
    PAYMENT_FAILED("Payment Failed", "Payment processing failed for subscription"),
    
    /**
     * Subscription is cancelled but still active until period ends
     */
    CANCELLED("Cancelled", "Subscription cancelled, active until period end"),
    
    /**
     * Subscription is paused by user request
     */
    PAUSED("Paused", "Subscription temporarily paused"),
    
    /**
     * Subscription upgrade is pending
     */
    UPGRADE_PENDING("Upgrade Pending", "Subscription upgrade in progress"),
    
    /**
     * Subscription downgrade is pending
     */
    DOWNGRADE_PENDING("Downgrade Pending", "Subscription downgrade scheduled"),
    
    /**
     * Subscription has been permanently cancelled
     */
    TERMINATED("Terminated", "Subscription permanently terminated");
    
    private final String displayName;
    private final String description;
    
    /**
     * Check if subscription provides access to features
     */
    public boolean hasAccess() {
        return this == ACTIVE || this == TRIAL || this == EXPIRED || this == CANCELLED;
    }
    
    /**
     * Check if subscription is in a billable state
     */
    public boolean isBillable() {
        return this == ACTIVE || this == EXPIRED || this == CANCELLED;
    }
    
    /**
     * Check if subscription can be upgraded
     */
    public boolean canUpgrade() {
        return this == ACTIVE || this == TRIAL;
    }
    
    /**
     * Check if subscription can be downgraded
     */
    public boolean canDowngrade() {
        return this == ACTIVE;
    }
    
    /**
     * Check if subscription can be cancelled
     */
    public boolean canCancel() {
        return this == ACTIVE || this == TRIAL || this == PAUSED;
    }
    
    /**
     * Check if subscription can be reactivated
     */
    public boolean canReactivate() {
        return this == SUSPENDED || this == PAUSED || this == EXPIRED || this == PAYMENT_FAILED;
    }
    
    /**
     * Check if subscription is in a final state
     */
    public boolean isFinalState() {
        return this == TERMINATED;
    }
    
    /**
     * Check if subscription requires payment
     */
    public boolean requiresPayment() {
        return this == PENDING || this == SUSPENDED || this == EXPIRED || this == PAYMENT_FAILED;
    }
    
    /**
     * Get valid transition states from current status
     */
    public SubscriptionStatus[] getValidTransitions() {
        return switch (this) {
            case PENDING -> new SubscriptionStatus[]{ACTIVE, TRIAL, SUSPENDED, PAYMENT_FAILED, TERMINATED};
            case ACTIVE -> new SubscriptionStatus[]{CANCELLED, SUSPENDED, PAUSED, PAYMENT_FAILED, UPGRADE_PENDING, DOWNGRADE_PENDING, EXPIRED};
            case TRIAL -> new SubscriptionStatus[]{ACTIVE, CANCELLED, SUSPENDED, PAYMENT_FAILED, EXPIRED};
            case EXPIRED -> new SubscriptionStatus[]{ACTIVE, SUSPENDED, TERMINATED};
            case SUSPENDED -> new SubscriptionStatus[]{ACTIVE, TERMINATED};
            case PAYMENT_FAILED -> new SubscriptionStatus[]{ACTIVE, SUSPENDED, TERMINATED};
            case CANCELLED -> new SubscriptionStatus[]{TERMINATED, ACTIVE}; // Can reactivate before termination
            case PAUSED -> new SubscriptionStatus[]{ACTIVE, CANCELLED, TERMINATED};
            case UPGRADE_PENDING -> new SubscriptionStatus[]{ACTIVE, SUSPENDED};
            case DOWNGRADE_PENDING -> new SubscriptionStatus[]{ACTIVE, SUSPENDED};
            case TERMINATED -> new SubscriptionStatus[]{}; // No transitions from terminated
        };
    }
    
    /**
     * Check if transition to target status is valid
     * MANDATORY: Rule #3 - No loops, using Stream API
     */
    public boolean canTransitionTo(SubscriptionStatus targetStatus) {
        return java.util.Arrays.stream(getValidTransitions())
            .anyMatch(validStatus -> validStatus == targetStatus);
    }
}