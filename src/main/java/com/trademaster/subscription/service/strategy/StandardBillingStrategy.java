package com.trademaster.subscription.service.strategy;

import com.trademaster.subscription.constants.PricingConstants;
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
            case PRO -> PricingConstants.PRO_MONTHLY_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_MONTHLY_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_MONTHLY_PRICE;
        };
    }
    
    private BigDecimal calculateQuarterlyAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> PricingConstants.PRO_QUARTERLY_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_QUARTERLY_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_QUARTERLY_PRICE;
        };
    }
    
    private BigDecimal calculateAnnualAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> PricingConstants.PRO_ANNUAL_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_ANNUAL_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_ANNUAL_PRICE;
        };
    }
    
    @Override
    public String getStrategyName() {
        return "Standard Billing Strategy";
    }
}