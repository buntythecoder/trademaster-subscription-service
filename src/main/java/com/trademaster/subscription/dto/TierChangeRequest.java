package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

/**
 * Tier Change Request DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TierChangeRequest(
    UUID subscriptionId,
    SubscriptionTier newTier,
    BillingCycle newBillingCycle,
    String reason,
    Boolean applyImmediately,
    String promotionCode
) {
    
    /**
     * Constructor with validation and defaults
     */
    public TierChangeRequest {
        // Validation using pattern matching
        switch (subscriptionId) {
            case null -> throw new IllegalArgumentException("Subscription ID is required");
            default -> {}
        }
        
        switch (newTier) {
            case null -> throw new IllegalArgumentException("New tier is required");
            default -> {}
        }
        
        // Set defaults using pattern matching
        applyImmediately = switch (applyImmediately) {
            case null -> true;
            default -> applyImmediately;
        };
    }
    
    /**
     * Check if change should be applied immediately
     */
    public boolean isApplyImmediately() {
        return applyImmediately != null && applyImmediately;
    }
    
    /**
     * Check if billing cycle is being changed
     */
    public boolean isBillingCycleChange() {
        return newBillingCycle != null;
    }
}