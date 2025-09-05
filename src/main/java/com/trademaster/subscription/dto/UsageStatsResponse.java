package com.trademaster.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Usage Statistics Response DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageStatsResponse(
    UUID userId,
    Map<String, Long> currentUsage,
    Map<String, Long> monthlyLimits,
    Map<String, Double> utilizationPercentage,
    LocalDateTime resetDate,
    String subscriptionTier,
    Long totalRequests,
    LocalDateTime periodStart,
    LocalDateTime periodEnd
) {
    
    /**
     * Factory method to create usage stats from components
     */
    public static UsageStatsResponse create(UUID userId, 
                                          Map<String, Long> currentUsage,
                                          Map<String, Long> monthlyLimits,
                                          String subscriptionTier,
                                          LocalDateTime periodStart,
                                          LocalDateTime periodEnd) {
        
        // Calculate utilization percentages using functional patterns
        Map<String, Double> utilizationPercentage = currentUsage.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> calculateUtilization(entry.getValue(), monthlyLimits.get(entry.getKey()))
            ));
        
        // Calculate total requests using functional patterns
        Long totalRequests = currentUsage.values().stream()
            .mapToLong(Long::longValue)
            .sum();
            
        return new UsageStatsResponse(
            userId,
            Map.copyOf(currentUsage),
            Map.copyOf(monthlyLimits),
            Map.copyOf(utilizationPercentage),
            periodEnd,
            subscriptionTier,
            totalRequests,
            periodStart,
            periodEnd
        );
    }
    
    /**
     * Calculate utilization percentage using pattern matching
     */
    private static Double calculateUtilization(Long current, Long limit) {
        return switch (limit) {
            case null -> 0.0;
            case Long l when l == -1 -> 0.0; // Unlimited
            case Long l when l == 0 -> 0.0; // No limit set
            case Long l -> Math.min(100.0, (current.doubleValue() / l.doubleValue()) * 100.0);
        };
    }
    
    /**
     * Check if any feature is over limit
     */
    public boolean hasOverLimitFeatures() {
        return utilizationPercentage.values().stream()
            .anyMatch(percentage -> percentage >= 100.0);
    }
    
    /**
     * Check if any feature is approaching limit (>80%)
     */
    public boolean hasApproachingLimitFeatures() {
        return utilizationPercentage.values().stream()
            .anyMatch(percentage -> percentage >= 80.0 && percentage < 100.0);
    }
}