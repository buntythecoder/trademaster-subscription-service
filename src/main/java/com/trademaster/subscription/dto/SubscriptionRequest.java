package com.trademaster.subscription.dto;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * Subscription Creation Request DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionRequest(
    UUID userId,
    SubscriptionTier tier,
    BillingCycle billingCycle,
    Boolean startTrial,
    String promotionCode,
    UUID paymentMethodId,
    Map<String, Object> metadata,
    Boolean autoRenewal
) {
    
    /**
     * Default constructor with validation
     */
    public SubscriptionRequest {
        // Validation using pattern matching
        switch (userId) {
            case null -> throw new IllegalArgumentException("User ID is required");
            default -> {}
        }
        
        switch (tier) {
            case null -> throw new IllegalArgumentException("Subscription tier is required");
            default -> {}
        }
        
        // Set defaults using pattern matching
        billingCycle = switch (billingCycle) {
            case null -> BillingCycle.MONTHLY;
            default -> billingCycle;
        };
        
        startTrial = switch (startTrial) {
            case null -> false;
            default -> startTrial;
        };
        
        autoRenewal = switch (autoRenewal) {
            case null -> true;
            default -> autoRenewal;
        };
    }
    
    /**
     * Check if trial should be started
     */
    public boolean isStartTrial() {
        return startTrial != null && startTrial;
    }
    
    /**
     * Check if auto-renewal is enabled
     */
    public boolean isAutoRenewal() {
        return autoRenewal != null && autoRenewal;
    }
}