package com.trademaster.subscription.service.strategy;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;

import java.math.BigDecimal;

/**
 * Strategy Pattern Interface for Billing Calculations
 * 
 * MANDATORY: Strategy Pattern - Advanced Design Pattern Rule #4
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@FunctionalInterface
public interface BillingCalculationStrategy {
    
    /**
     * Calculate billing amount for given tier and cycle
     */
    BigDecimal calculateAmount(SubscriptionTier tier, BillingCycle cycle);
    
    /**
     * Get strategy name for logging and metrics
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }
}