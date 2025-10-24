package com.trademaster.subscription.service.strategy;

import com.trademaster.subscription.constants.PricingConstants;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Promotional Billing Calculation Strategy Implementation
 * 
 * MANDATORY: Strategy Pattern - Advanced Design Pattern Rule #4
 * MANDATORY: Pattern Matching - Rule #14
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component("promotionalBillingStrategy")
public class PromotionalBillingStrategy implements BillingCalculationStrategy {

    private static final BigDecimal PROMOTIONAL_DISCOUNT = PricingConstants.PROMOTIONAL_DISCOUNT_PERCENT;

    @Override
    public BigDecimal calculateAmount(SubscriptionTier tier, BillingCycle cycle) {
        BigDecimal baseAmount = switch (cycle) {
            case MONTHLY -> calculateBaseMonthlyAmount(tier);
            case QUARTERLY -> calculateBaseQuarterlyAmount(tier);
            case ANNUAL -> calculateBaseAnnualAmount(tier);
        };
        
        return applyPromotionalDiscount(baseAmount);
    }
    
    private BigDecimal calculateBaseMonthlyAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> PricingConstants.PRO_MONTHLY_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_MONTHLY_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_MONTHLY_PRICE;
        };
    }
    
    private BigDecimal calculateBaseQuarterlyAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> PricingConstants.PRO_QUARTERLY_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_QUARTERLY_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_QUARTERLY_PRICE;
        };
    }
    
    private BigDecimal calculateBaseAnnualAmount(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> PricingConstants.PRO_ANNUAL_PRICE;
            case AI_PREMIUM -> PricingConstants.AI_PREMIUM_ANNUAL_PRICE;
            case INSTITUTIONAL -> PricingConstants.INSTITUTIONAL_ANNUAL_PRICE;
        };
    }
    
    private BigDecimal applyPromotionalDiscount(BigDecimal baseAmount) {
        return switch (baseAmount.compareTo(BigDecimal.ZERO)) {
            case 0 -> BigDecimal.ZERO; // No discount on free tier
            default -> baseAmount.multiply(BigDecimal.ONE.subtract(PROMOTIONAL_DISCOUNT))
                .setScale(2, RoundingMode.HALF_UP);
        };
    }
    
    @Override
    public String getStrategyName() {
        return "Promotional Billing Strategy (20% Discount)";
    }
}