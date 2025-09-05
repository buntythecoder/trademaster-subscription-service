package com.trademaster.subscription.service.strategy;

import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Standard Billing Calculation Strategy Implementation
 * 
 * MANDATORY: Strategy Pattern - Advanced Design Pattern Rule #4
 * MANDATORY: Pattern Matching - Rule #14
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component("standardBillingStrategy")
public class StandardBillingStrategy implements BillingCalculationStrategy {

    @Override
    public BigDecimal calculateAmount(SubscriptionTier tier, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> calculateMonthlyAmount(tier);
            case QUARTERLY -> calculateQuarterlyAmount(tier);
            case ANNUAL -> calculateAnnualAmount(tier);
        };
    }
    
    private BigDecimal calculateMonthlyAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> new BigDecimal("29.99");
            case AI_PREMIUM -> new BigDecimal("99.99");
            case INSTITUTIONAL -> new BigDecimal("299.99");
        };
    }
    
    private BigDecimal calculateQuarterlyAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> new BigDecimal("79.99"); // 10% discount
            case AI_PREMIUM -> new BigDecimal("269.99"); // 10% discount
            case INSTITUTIONAL -> new BigDecimal("809.99"); // 10% discount
        };
    }
    
    private BigDecimal calculateAnnualAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> new BigDecimal("299.99"); // 17% discount
            case AI_PREMIUM -> new BigDecimal("999.99"); // 17% discount
            case INSTITUTIONAL -> new BigDecimal("2999.99"); // 17% discount
        };
    }
    
    @Override
    public String getStrategyName() {
        return "Standard Billing Strategy";
    }
}