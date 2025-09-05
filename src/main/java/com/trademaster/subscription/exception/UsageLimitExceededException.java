package com.trademaster.subscription.exception;

import java.util.UUID;

/**
 * Usage Limit Exceeded Exception
 * 
 * Thrown when a user attempts to exceed their subscription usage limits.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class UsageLimitExceededException extends SubscriptionException {

    private final UUID userId;
    private final String featureName;
    private final Long currentUsage;
    private final Long usageLimit;

    public UsageLimitExceededException(UUID userId, String featureName, Long currentUsage, Long usageLimit) {
        super(String.format("Usage limit exceeded for user %s, feature '%s': %d/%d", 
              userId, featureName, currentUsage, usageLimit), 
              "USAGE_LIMIT_EXCEEDED");
        this.userId = userId;
        this.featureName = featureName;
        this.currentUsage = currentUsage;
        this.usageLimit = usageLimit;
    }

    public UsageLimitExceededException(UUID userId, String featureName, String customMessage) {
        super(customMessage, "USAGE_LIMIT_EXCEEDED");
        this.userId = userId;
        this.featureName = featureName;
        this.currentUsage = null;
        this.usageLimit = null;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFeatureName() {
        return featureName;
    }

    public Long getCurrentUsage() {
        return currentUsage;
    }

    public Long getUsageLimit() {
        return usageLimit;
    }
}