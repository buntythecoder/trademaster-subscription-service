package com.trademaster.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

/**
 * Usage Increment Request DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageIncrementRequest(
    UUID userId,
    String feature,
    Long amount,
    String description
) {
    
    /**
     * Constructor with validation and defaults
     */
    public UsageIncrementRequest {
        // Validation using pattern matching
        switch (userId) {
            case null -> throw new IllegalArgumentException("User ID is required");
            default -> {}
        }
        
        switch (feature) {
            case "" -> throw new IllegalArgumentException("Feature name is required");
            case null -> throw new IllegalArgumentException("Feature name is required");
            default -> {}
        }
        
        // Set defaults and validate using pattern matching
        amount = switch (amount) {
            case null -> 1L;
            case Long value when value <= 0 -> throw new IllegalArgumentException("Amount must be positive");
            default -> amount;
        };
    }
    
    /**
     * Check if description is provided
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}