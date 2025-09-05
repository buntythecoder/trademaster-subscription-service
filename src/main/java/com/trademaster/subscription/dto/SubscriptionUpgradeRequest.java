package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Subscription Upgrade Request DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionUpgradeRequest(
    SubscriptionTier newTier,
    SubscriptionTier targetTier,
    BillingCycle billingCycle,
    Boolean immediateUpgrade,
    String promotionCode
) {
    
    /**
     * Constructor with validation and defaults
     */
    public SubscriptionUpgradeRequest {
        // Validation using pattern matching
        switch (newTier) {
            case null -> throw new IllegalArgumentException("New tier is required");
            default -> {}
        }
        
        // Set defaults using pattern matching
        targetTier = switch (targetTier) {
            case null -> newTier;
            default -> targetTier;
        };
        
        immediateUpgrade = switch (immediateUpgrade) {
            case null -> true;
            default -> immediateUpgrade;
        };
    }
    
    /**
     * Get the new tier (for backward compatibility)
     */
    public SubscriptionTier getNewTier() {
        return newTier;
    }
    
    /**
     * Check if upgrade should be immediate
     */
    public boolean isImmediateUpgrade() {
        return immediateUpgrade != null && immediateUpgrade;
    }
}