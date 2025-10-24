package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Tier Comparison Service
 * MANDATORY: Single Responsibility - Handles tier validation and comparison only
 * MANDATORY: Functional Programming - Pattern matching for tier operations
 *
 * Validates tier upgrade/downgrade operations and compares tier rankings.
 * Provides functional comparison logic for subscription tier management.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class TierComparisonService {

    /**
     * Validate tier upgrade request
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public Result<Subscription, String> validateTierUpgrade(Subscription subscription, SubscriptionTier newTier) {
        SubscriptionTier currentTier = subscription.getTier();

        return switch (compareTiers(currentTier, newTier)) {
            case "UPGRADE" -> Result.success(subscription);
            case "SAME" -> Result.failure("Already on requested tier: " + newTier);
            case "DOWNGRADE" -> Result.failure("Cannot downgrade from " + currentTier + " to " + newTier);
            default -> Result.failure("Invalid tier upgrade request");
        };
    }

    /**
     * Compare two tiers to determine upgrade/downgrade/same
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public String compareTiers(SubscriptionTier current, SubscriptionTier target) {
        int currentRank = getTierRank(current);
        int targetRank = getTierRank(target);

        return switch (Integer.compare(targetRank, currentRank)) {
            case 1 -> "UPGRADE";
            case 0 -> "SAME";
            case -1 -> "DOWNGRADE";
            default -> "INVALID";
        };
    }

    /**
     * Get tier rank for comparison
     * MANDATORY: Max 15 lines, complexity ≤7
     * MANDATORY: Pattern Matching - Rule #14
     */
    private int getTierRank(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> 1;
            case PRO -> 2;
            case AI_PREMIUM -> 3;
            case INSTITUTIONAL -> 4;
        };
    }
}
