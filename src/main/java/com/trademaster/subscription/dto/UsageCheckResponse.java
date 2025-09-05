package com.trademaster.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Usage Check Response DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageCheckResponse(
    Boolean canUse,
    UUID userId,
    String featureName,
    Long currentUsage,
    Long usageLimit,
    Long remainingUsage,
    Double usagePercentage,
    Boolean limitExceeded,
    Boolean isUnlimited,
    UsageTrackingResponse.WarningLevel warningLevel,
    String warningMessage,
    String subscriptionTier,
    Boolean usageIncremented
) {

    /**
     * Factory method to create a successful response using pattern matching
     */
    public static UsageCheckResponse allowed(UUID userId, String featureName, 
                                           Long currentUsage, Long usageLimit, 
                                           String subscriptionTier, boolean incremented) {
        
        // Calculate remaining usage using pattern matching
        Long remainingUsage = switch (usageLimit) {
            case Long limit when limit == -1 -> Long.MAX_VALUE;
            case Long limit -> Math.max(0L, limit - currentUsage);
        };
        
        // Calculate usage percentage using pattern matching
        Double usagePercentage = switch (usageLimit) {
            case Long limit when limit == -1 -> 0.0;
            case Long limit -> Math.min(100.0, (currentUsage.doubleValue() / limit.doubleValue()) * 100.0);
        };
        
        return new UsageCheckResponse(
            true,
            userId,
            featureName,
            currentUsage,
            usageLimit,
            remainingUsage,
            usagePercentage,
            false,
            usageLimit == -1,
            calculateWarningLevel(currentUsage, usageLimit),
            null,
            subscriptionTier,
            incremented
        );
    }

    /**
     * Factory method to create a denied response using pattern matching
     */
    public static UsageCheckResponse denied(UUID userId, String featureName, 
                                          Long currentUsage, Long usageLimit, 
                                          String reason, String subscriptionTier) {
        return new UsageCheckResponse(
            false,
            userId,
            featureName,
            currentUsage,
            usageLimit,
            0L,
            100.0,
            true,
            false,
            UsageTrackingResponse.WarningLevel.CRITICAL,
            reason,
            subscriptionTier,
            false
        );
    }

    /**
     * Factory method to create response for no active subscription
     */
    public static UsageCheckResponse noSubscription(UUID userId, String featureName) {
        return new UsageCheckResponse(
            false,
            userId,
            featureName,
            0L,
            0L,
            0L,
            0.0,
            false,
            false,
            UsageTrackingResponse.WarningLevel.NONE,
            "No active subscription found",
            "NONE",
            false
        );
    }

    /**
     * Calculate warning level using pattern matching
     */
    private static UsageTrackingResponse.WarningLevel calculateWarningLevel(Long currentUsage, Long usageLimit) {
        return switch (usageLimit) {
            case Long limit when limit == -1 -> UsageTrackingResponse.WarningLevel.NONE;
            case Long limit -> {
                double percentage = (currentUsage.doubleValue() / limit.doubleValue()) * 100.0;
                yield switch (percentage) {
                    case double p when p >= 100.0 -> UsageTrackingResponse.WarningLevel.CRITICAL;
                    case double p when p >= 90.0 -> UsageTrackingResponse.WarningLevel.HIGH;
                    case double p when p >= 80.0 -> UsageTrackingResponse.WarningLevel.MEDIUM;
                    case double p when p >= 60.0 -> UsageTrackingResponse.WarningLevel.LOW;
                    default -> UsageTrackingResponse.WarningLevel.NONE;
                };
            }
        };
    }
}