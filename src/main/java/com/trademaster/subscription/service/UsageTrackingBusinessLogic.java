package com.trademaster.subscription.service;

import com.trademaster.subscription.entity.UsageTracking;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Usage Tracking Business Logic
 * MANDATORY: Single Responsibility - Usage tracking business rules only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Extracted from UsageTracking entity to maintain clean separation of concerns.
 * Handles all business logic related to usage limits, tracking, and warnings.
 *
 * @author TradeMaster Development Team
 */
@Service
public class UsageTrackingBusinessLogic {

    /**
     * Check if feature has unlimited usage
     */
    public boolean isUnlimited(UsageTracking usage) {
        return usage.getUsageLimit() == -1;
    }

    /**
     * Check if usage is within limits
     */
    public boolean isWithinLimit(UsageTracking usage) {
        return isUnlimited(usage) || usage.getUsageCount() < usage.getUsageLimit();
    }

    /**
     * Get remaining usage allowance
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public long getRemainingUsage(UsageTracking usage) {
        return Optional.of(isUnlimited(usage))
            .filter(Boolean::booleanValue)
            .map(unlimited -> Long.MAX_VALUE)
            .orElseGet(() -> Math.max(0, usage.getUsageLimit() - usage.getUsageCount()));
    }

    /**
     * Get usage percentage (0-100)
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public double getUsagePercentage(UsageTracking usage) {
        return Optional.of(isUnlimited(usage) || usage.getUsageLimit() == 0)
            .filter(shouldReturnZero -> !shouldReturnZero)
            .map(valid -> Math.min(100.0,
                (usage.getUsageCount().doubleValue() / usage.getUsageLimit().doubleValue()) * 100.0))
            .orElse(0.0);
    }

    /**
     * Check if usage period is active
     */
    public boolean isPeriodActive(UsageTracking usage) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(usage.getPeriodStart()) && now.isBefore(usage.getPeriodEnd());
    }

    /**
     * Check if usage needs reset
     */
    public boolean needsReset(UsageTracking usage) {
        return LocalDateTime.now().isAfter(usage.getResetDate());
    }

    /**
     * Increment usage count
     * Returns true if increment succeeded without exceeding limit
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public boolean incrementUsage(UsageTracking usage, long amount) {
        usage.setUsageCount(usage.getUsageCount() + amount);

        return Optional.of(isUnlimited(usage))
            .filter(Boolean::booleanValue)
            .map(unlimited -> true)
            .orElseGet(() -> {
                boolean withinLimit = usage.getUsageCount() <= usage.getUsageLimit();
                Optional.of(withinLimit)
                    .filter(within -> !within)
                    .ifPresent(exceeded -> {
                        usage.setLimitExceeded(true);
                        usage.setExceededCount(usage.getExceededCount() + 1);
                        Optional.ofNullable(usage.getFirstExceededAt())
                            .or(() -> {
                                usage.setFirstExceededAt(LocalDateTime.now());
                                return Optional.empty();
                            });
                    });
                return withinLimit;
            });
    }

    /**
     * Reset usage counter for new period
     */
    public void resetUsage(UsageTracking usage) {
        usage.setUsageCount(0L);
        usage.setLimitExceeded(false);
        usage.setExceededCount(0);
        usage.setFirstExceededAt(null);

        LocalDateTime now = LocalDateTime.now();
        usage.setPeriodStart(now);
        usage.setPeriodEnd(now.plusDays(usage.getResetFrequencyDays()));
        usage.setResetDate(usage.getPeriodEnd());
    }

    /**
     * Update usage limit (when subscription tier changes)
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public void updateLimit(UsageTracking usage, long newLimit) {
        usage.setUsageLimit(newLimit);

        usage.setLimitExceeded(
            Optional.of(newLimit == -1)
                .filter(Boolean::booleanValue)
                .map(unlimited -> false)
                .orElseGet(() -> usage.getUsageCount() > newLimit)
        );
    }

    /**
     * Check if user is approaching limit (>80% usage)
     */
    public boolean isApproachingLimit(UsageTracking usage) {
        return !isUnlimited(usage) && getUsagePercentage(usage) > 80.0;
    }

    /**
     * Check if user has hit soft limit (>90% usage)
     */
    public boolean isAtSoftLimit(UsageTracking usage) {
        return !isUnlimited(usage) && getUsagePercentage(usage) > 90.0;
    }

    /**
     * Get warning level based on usage percentage
     * MANDATORY: Rule #3 - No if-else chains, using Stream API
     */
    public UsageWarningLevel getWarningLevel(UsageTracking usage) {
        return Optional.of(isUnlimited(usage))
            .filter(Boolean::booleanValue)
            .map(unlimited -> UsageWarningLevel.NONE)
            .orElseGet(() -> {
                double percentage = getUsagePercentage(usage);
                return java.util.Arrays.stream(UsageWarningLevel.values())
                    .filter(level -> percentage >= level.getThreshold())
                    .max(java.util.Comparator.comparingInt(UsageWarningLevel::getThreshold))
                    .orElse(UsageWarningLevel.NONE);
            });
    }

    /**
     * Usage warning levels
     */
    public enum UsageWarningLevel {
        NONE("No Warning", 0),
        LOW("Low Usage", 60),
        MEDIUM("Medium Usage", 80),
        HIGH("High Usage", 90),
        CRITICAL("Limit Exceeded", 100);

        private final String description;
        private final int threshold;

        UsageWarningLevel(String description, int threshold) {
            this.description = description;
            this.threshold = threshold;
        }

        public String getDescription() {
            return description;
        }

        public int getThreshold() {
            return threshold;
        }
    }
}
