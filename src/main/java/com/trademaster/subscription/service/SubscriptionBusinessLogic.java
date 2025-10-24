package com.trademaster.subscription.service;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Subscription Business Logic Service
 * MANDATORY: Single Responsibility - Business rules and calculations only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Extracted business logic from Subscription entity to maintain clean separation.
 * Pure functions that operate on subscription state without side effects where possible.
 *
 * @author TradeMaster Development Team
 */
@Service
public class SubscriptionBusinessLogic {

    /**
     * Check if subscription is currently active
     */
    public boolean isActive(Subscription subscription) {
        return subscription.getStatus().hasAccess() &&
               (subscription.getEndDate() == null ||
                subscription.getEndDate().isAfter(LocalDateTime.now()));
    }

    /**
     * Check if subscription is in trial period
     */
    public boolean isInTrial(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.TRIAL &&
               subscription.getTrialEndDate() != null &&
               subscription.getTrialEndDate().isAfter(LocalDateTime.now());
    }

    /**
     * Check if subscription has expired
     */
    public boolean isExpired(Subscription subscription) {
        return subscription.getEndDate() != null &&
               subscription.getEndDate().isBefore(LocalDateTime.now());
    }

    /**
     * Check if subscription can be billed
     */
    public boolean canBeBilled(Subscription subscription) {
        return subscription.getStatus().isBillable() &&
               subscription.getAutoRenewal() &&
               subscription.getNextBillingDate() != null;
    }

    /**
     * Check if subscription is due for billing
     */
    public boolean isDueForBilling(Subscription subscription) {
        return canBeBilled(subscription) &&
               subscription.getNextBillingDate().isBefore(LocalDateTime.now());
    }

    /**
     * Check if subscription is in grace period (3 days after billing)
     */
    public boolean isInGracePeriod(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.EXPIRED &&
               subscription.getNextBillingDate() != null &&
               subscription.getNextBillingDate().plusDays(3).isAfter(LocalDateTime.now());
    }

    /**
     * Calculate days remaining in current billing cycle
     */
    public long getDaysRemainingInCycle(Subscription subscription) {
        return subscription.getNextBillingDate() == null ? 0 :
               Duration.between(LocalDateTime.now(), subscription.getNextBillingDate()).toDays();
    }

    /**
     * Calculate monthly savings compared to monthly billing
     * MANDATORY: Rule #3 - No if-else, using functional pattern
     */
    public BigDecimal getMonthlySavings(Subscription subscription) {
        return Optional.of(subscription.getBillingCycle())
            .filter(cycle -> cycle != BillingCycle.MONTHLY)
            .map(cycle -> {
                BigDecimal effectiveMonthly = subscription.getBillingAmount().divide(
                    BigDecimal.valueOf(cycle.getMonths()),
                    2,
                    RoundingMode.HALF_UP
                );
                return subscription.getMonthlyPrice().subtract(effectiveMonthly);
            })
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Activate subscription - mutates state
     * MANDATORY: Rule #3 - No if-else, using pattern matching
     */
    public void activate(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        switch (subscription.getStatus()) {
            case TRIAL -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setActivatedDate(now);
            }
            default -> Optional.of(subscription.getStatus())
                .filter(status -> status.canTransitionTo(SubscriptionStatus.ACTIVE))
                .ifPresent(status -> {
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setStartDate(now);
                    subscription.setActivatedDate(now);
                    subscription.setFailedBillingAttempts(0);
                });
        }
    }

    /**
     * Cancel subscription - mutates state
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public void cancel(Subscription subscription, String reason) {
        Optional.of(subscription.getStatus())
            .filter(SubscriptionStatus::canCancel)
            .ifPresent(status -> {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setCancellationReason(reason);
                subscription.setCancelledAt(LocalDateTime.now());
                subscription.setAutoRenewal(false);
            });
    }

    /**
     * Suspend subscription - mutates state
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public void suspend(Subscription subscription) {
        Optional.of(subscription.getStatus())
            .filter(status -> status.canTransitionTo(SubscriptionStatus.SUSPENDED))
            .ifPresent(status -> subscription.setStatus(SubscriptionStatus.SUSPENDED));
    }

    /**
     * Update next billing date based on billing cycle
     * MANDATORY: Rule #3 - No if-else, using Optional chaining
     */
    public void updateNextBillingDate(Subscription subscription) {
        Optional.ofNullable(subscription.getNextBillingDate())
            .or(() -> Optional.ofNullable(subscription.getStartDate()))
            .ifPresent(date -> subscription.setNextBillingDate(
                subscription.getBillingCycle().getNextBillingDate(date)
            ));
    }

    /**
     * Record successful billing - mutates state
     * MANDATORY: Rule #3 - No if-else, using pattern matching
     */
    public void recordSuccessfulBilling(Subscription subscription) {
        subscription.setLastBilledDate(LocalDateTime.now());
        subscription.setFailedBillingAttempts(0);
        updateNextBillingDate(subscription);

        switch (subscription.getStatus()) {
            case SUSPENDED, EXPIRED -> subscription.setStatus(SubscriptionStatus.ACTIVE);
            default -> {} // No status change needed
        }
    }

    /**
     * Record failed billing attempt - mutates state
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    public void recordFailedBilling(Subscription subscription) {
        subscription.setFailedBillingAttempts(subscription.getFailedBillingAttempts() + 1);

        Optional.of(subscription.getFailedBillingAttempts())
            .filter(attempts -> attempts >= 3)
            .ifPresent(attempts -> subscription.setStatus(SubscriptionStatus.SUSPENDED));
    }
}
