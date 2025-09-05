package com.trademaster.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trademaster.subscription.entity.UsageTracking;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usage Tracking Response DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageTrackingResponse(
    UUID id,
    UUID userId,
    UUID subscriptionId,
    String featureName,
    Long usageCount,
    Long usageLimit,
    LocalDateTime periodStart,
    LocalDateTime periodEnd,
    LocalDateTime resetDate,
    Integer resetFrequencyDays,
    Boolean limitExceeded,
    Integer exceededCount,
    LocalDateTime firstExceededAt,
    Boolean isUnlimited,
    Boolean isWithinLimit,
    Long remainingUsage,
    Double usagePercentage,
    Boolean isPeriodActive,
    Boolean isApproachingLimit,
    Boolean isAtSoftLimit,
    WarningLevel warningLevel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * Factory method to create response from entity using pattern matching
     */
    public static UsageTrackingResponse fromEntity(UsageTracking entity) {
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate derived values using pattern matching
        Long remainingUsage = switch (entity.getUsageLimit()) {
            case Long limit when limit == -1 -> Long.MAX_VALUE;  // Unlimited
            case Long limit -> Math.max(0L, limit - entity.getUsageCount());
        };
        
        Double usagePercentage = switch (entity.getUsageLimit()) {
            case Long limit when limit == -1 -> 0.0;  // Unlimited
            case Long limit when limit == 0 -> 0.0;   // No limit
            case Long limit -> Math.min(100.0, (entity.getUsageCount().doubleValue() / limit.doubleValue()) * 100.0);
        };
        
        Boolean isUnlimited = entity.getUsageLimit() == -1;
        Boolean limitExceeded = !isUnlimited && entity.getUsageCount() > entity.getUsageLimit();
        Boolean isWithinLimit = !limitExceeded;
        Boolean isPeriodActive = entity.getBillingPeriodStart().isBefore(now) && entity.getBillingPeriodEnd().isAfter(now);
        Boolean isApproachingLimit = !isUnlimited && usagePercentage >= 80.0 && usagePercentage < 100.0;
        Boolean isAtSoftLimit = !isUnlimited && usagePercentage >= 90.0 && usagePercentage < 100.0;
        
        WarningLevel warningLevel = calculateWarningLevel(usagePercentage, limitExceeded);
        
        return new UsageTrackingResponse(
            entity.getId(),
            entity.getUserId(),
            entity.getSubscriptionId(),
            entity.getFeature(),
            entity.getUsageCount(),
            entity.getUsageLimit(),
            entity.getBillingPeriodStart(),
            entity.getBillingPeriodEnd(),
            entity.getLastResetDate(),
            null, // resetFrequencyDays not in entity
            limitExceeded,
            null, // exceededCount not in entity
            null, // firstExceededAt not in entity
            isUnlimited,
            isWithinLimit,
            remainingUsage,
            usagePercentage,
            isPeriodActive,
            isApproachingLimit,
            isAtSoftLimit,
            warningLevel,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Calculate warning level using pattern matching
     */
    private static WarningLevel calculateWarningLevel(Double percentage, Boolean limitExceeded) {
        return switch (limitExceeded) {
            case Boolean exceeded when exceeded -> WarningLevel.CRITICAL;
            default -> switch (percentage) {
                case Double p when p >= 90.0 -> WarningLevel.HIGH;
                case Double p when p >= 80.0 -> WarningLevel.MEDIUM;
                case Double p when p >= 60.0 -> WarningLevel.LOW;
                default -> WarningLevel.NONE;
            };
        };
    }
    
    /**
     * Usage warning levels using sealed interface pattern
     */
    public enum WarningLevel {
        NONE("No Warning", 0),
        LOW("Low Usage", 60),
        MEDIUM("Medium Usage", 80),
        HIGH("High Usage", 90),
        CRITICAL("Limit Exceeded", 100);

        private final String description;
        private final int threshold;

        WarningLevel(String description, int threshold) {
            this.description = description;
            this.threshold = threshold;
        }

        public String getDescription() {
            return description;
        }

        public int getThreshold() {
            return threshold;
        }
        
        /**
         * Get warning level from percentage using pattern matching
         */
        public static WarningLevel fromPercentage(double percentage) {
            return switch (percentage) {
                case double p when p >= 100.0 -> CRITICAL;
                case double p when p >= 90.0 -> HIGH;
                case double p when p >= 80.0 -> MEDIUM;
                case double p when p >= 60.0 -> LOW;
                default -> NONE;
            };
        }
    }
}