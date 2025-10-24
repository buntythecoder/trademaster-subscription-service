package com.trademaster.subscription.service;

import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionHistoryChangeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Subscription History Business Logic
 * MANDATORY: Single Responsibility - History analysis and interpretation only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Extracted from SubscriptionHistory entity to maintain clean separation of concerns.
 * Handles all business logic related to analyzing subscription change history.
 *
 * @author TradeMaster Development Team
 */
@Service
public class SubscriptionHistoryBusinessLogic {

    /**
     * Check if this was a tier upgrade
     */
    public boolean isUpgrade(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.UPGRADED ||
               (history.getOldTier() != null && history.getNewTier() != null &&
                history.getOldTier().ordinal() < history.getNewTier().ordinal());
    }

    /**
     * Check if this was a tier downgrade
     */
    public boolean isDowngrade(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.DOWNGRADED ||
               (history.getOldTier() != null && history.getNewTier() != null &&
                history.getOldTier().ordinal() > history.getNewTier().ordinal());
    }

    /**
     * Check if this was a billing cycle change
     */
    public boolean isBillingCycleChange(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.BILLING_CYCLE_CHANGED ||
               (history.getOldBillingCycle() != null && history.getNewBillingCycle() != null &&
                history.getOldBillingCycle() != history.getNewBillingCycle());
    }

    /**
     * Check if this was a price change
     */
    public boolean isPriceChange(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.PRICE_CHANGED ||
               (history.getOldBillingAmount() != null && history.getNewBillingAmount() != null &&
                history.getOldBillingAmount().compareTo(history.getNewBillingAmount()) != 0);
    }

    /**
     * Calculate revenue impact of this change
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public BigDecimal getRevenueImpact(SubscriptionHistory history) {
        return Optional.ofNullable(history.getNewBillingAmount())
            .flatMap(newAmount -> Optional.ofNullable(history.getOldBillingAmount())
                .map(newAmount::subtract))
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Check if this change affects billing
     */
    public boolean affectsBilling(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.UPGRADED ||
               history.getChangeType() == SubscriptionHistoryChangeType.DOWNGRADED ||
               history.getChangeType() == SubscriptionHistoryChangeType.BILLING_CYCLE_CHANGED ||
               history.getChangeType() == SubscriptionHistoryChangeType.PRICE_CHANGED ||
               history.getChangeType() == SubscriptionHistoryChangeType.PROMOTION_APPLIED ||
               history.getChangeType() == SubscriptionHistoryChangeType.PROMOTION_REMOVED;
    }

    /**
     * Check if this is a cancellation event
     */
    public boolean isCancellation(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.CANCELLED ||
               history.getChangeType() == SubscriptionHistoryChangeType.TERMINATED ||
               (history.getNewStatus() != null &&
                (history.getNewStatus() == SubscriptionStatus.CANCELLED ||
                 history.getNewStatus() == SubscriptionStatus.TERMINATED));
    }

    /**
     * Check if this is a reactivation event
     */
    public boolean isReactivation(SubscriptionHistory history) {
        return history.getChangeType() == SubscriptionHistoryChangeType.REACTIVATED ||
               history.getChangeType() == SubscriptionHistoryChangeType.RESUMED ||
               (history.getOldStatus() != null && history.getNewStatus() != null &&
                !history.getOldStatus().hasAccess() && history.getNewStatus().hasAccess());
    }

    /**
     * Get human-readable description of the change
     * MANDATORY: Rule #3 - No ternary/if-else, using Optional pattern
     */
    public String getChangeDescription(SubscriptionHistory history) {
        StringBuilder description = new StringBuilder();

        switch (history.getChangeType()) {
            case UPGRADED:
                description.append("Upgraded from ")
                          .append(Optional.ofNullable(history.getOldTier())
                                        .map(tier -> tier.getDisplayName())
                                        .orElse("Unknown"))
                          .append(" to ")
                          .append(Optional.ofNullable(history.getNewTier())
                                        .map(tier -> tier.getDisplayName())
                                        .orElse("Unknown"));
                break;
            case DOWNGRADED:
                description.append("Downgraded from ")
                          .append(Optional.ofNullable(history.getOldTier())
                                        .map(tier -> tier.getDisplayName())
                                        .orElse("Unknown"))
                          .append(" to ")
                          .append(Optional.ofNullable(history.getNewTier())
                                        .map(tier -> tier.getDisplayName())
                                        .orElse("Unknown"));
                break;
            case BILLING_CYCLE_CHANGED:
                description.append("Changed billing cycle from ")
                          .append(Optional.ofNullable(history.getOldBillingCycle())
                                        .map(cycle -> cycle.getDisplayName())
                                        .orElse("Unknown"))
                          .append(" to ")
                          .append(Optional.ofNullable(history.getNewBillingCycle())
                                        .map(cycle -> cycle.getDisplayName())
                                        .orElse("Unknown"));
                break;
            default:
                description.append(history.getChangeType().getDescription());
        }

        Optional.ofNullable(history.getChangeReason())
            .filter(reason -> !reason.trim().isEmpty())
            .ifPresent(reason -> description.append(" - ").append(reason));

        return description.toString();
    }
}
