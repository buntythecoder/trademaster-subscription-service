package com.trademaster.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

/**
 * Usage Check Request DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageCheckRequest(
    UUID userId,
    String featureName,
    Long usageAmount,
    Boolean incrementUsage
) {
    
    /**
     * Constructor with validation and defaults
     */
    public UsageCheckRequest {
        // Validation using pattern matching
        switch (userId) {
            case null -> throw new IllegalArgumentException("User ID is required");
            default -> {}
        }
        
        // MANDATORY: Rule #3 - No if-else, using pattern matching
        switch (featureName) {
            case null -> throw new IllegalArgumentException("Feature name is required");
            case String name when name.trim().isEmpty() -> throw new IllegalArgumentException("Feature name is required");
            default -> {}
        }
        
        // Set defaults using pattern matching
        usageAmount = switch (usageAmount) {
            case null -> 1L;
            case Long value when value < 1 -> throw new IllegalArgumentException("Usage amount must be at least 1");
            default -> usageAmount;
        };
        
        incrementUsage = switch (incrementUsage) {
            case null -> false;
            default -> incrementUsage;
        };
    }
    
    /**
     * Check if usage should be incremented
     * MANDATORY: Rule #3 - No compound boolean with null check, using Optional
     */
    public boolean isIncrementUsage() {
        return java.util.Optional.ofNullable(incrementUsage)
            .orElse(false);
    }
    
    /**
     * Check if this is a check-only request
     */
    public boolean isCheckOnly() {
        return !isIncrementUsage();
    }
}