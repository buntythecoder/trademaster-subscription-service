package com.trademaster.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.Period;

/**
 * Billing Cycle Enumeration
 * 
 * Defines the available billing cycles for subscriptions
 * with their periods and discount calculations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum BillingCycle {
    
    MONTHLY("Monthly", Period.ofMonths(1), 0.0) {
        @Override
        public LocalDateTime getNextBillingDate(LocalDateTime currentDate) {
            return currentDate.plusMonths(1);
        }
    },
    
    QUARTERLY("Quarterly", Period.ofMonths(3), 0.06) {
        @Override
        public LocalDateTime getNextBillingDate(LocalDateTime currentDate) {
            return currentDate.plusMonths(3);
        }
    },
    
    ANNUAL("Annual", Period.ofYears(1), 0.17) {
        @Override
        public LocalDateTime getNextBillingDate(LocalDateTime currentDate) {
            return currentDate.plusYears(1);
        }
    };
    
    private final String displayName;
    private final Period period;
    private final double discountPercentage;
    
    /**
     * Calculate the next billing date from the given date
     */
    public abstract LocalDateTime getNextBillingDate(LocalDateTime currentDate);
    
    /**
     * Get the number of months in this billing cycle
     */
    public int getMonths() {
        return switch (this) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case ANNUAL -> 12;
        };
    }
    
    /**
     * Check if this cycle offers a discount
     */
    public boolean hasDiscount() {
        return discountPercentage > 0;
    }
    
    /**
     * Get the discount multiplier (1.0 - discount percentage)
     */
    public double getDiscountMultiplier() {
        return 1.0 - discountPercentage;
    }
    
    /**
     * Calculate the total price for this billing cycle
     * considering the discount
     */
    public double calculateTotalPrice(double monthlyPrice) {
        return monthlyPrice * getMonths() * getDiscountMultiplier();
    }
    
    /**
     * Get the effective monthly price after discount
     */
    public double getEffectiveMonthlyPrice(double monthlyPrice) {
        return calculateTotalPrice(monthlyPrice) / getMonths();
    }
    
    /**
     * Get savings per month compared to monthly billing
     */
    public double getMonthlySavings(double monthlyPrice) {
        return monthlyPrice - getEffectiveMonthlyPrice(monthlyPrice);
    }
    
    /**
     * Get total savings compared to monthly billing
     */
    public double getTotalSavings(double monthlyPrice) {
        return getMonthlySavings(monthlyPrice) * getMonths();
    }
    
    /**
     * Check if this is a long-term commitment cycle
     */
    public boolean isLongTerm() {
        return this == QUARTERLY || this == ANNUAL;
    }
    
    /**
     * Get the recommended billing cycle based on savings threshold
     * MANDATORY: Rule #3 - No if-else, using Stream API
     */
    public static BillingCycle getRecommendedCycle(double savingsThreshold) {
        return java.util.Arrays.stream(values())
            .filter(cycle -> cycle.getDiscountPercentage() >= savingsThreshold)
            .max(java.util.Comparator.comparingDouble(BillingCycle::getDiscountPercentage))
            .orElse(MONTHLY);
    }
}