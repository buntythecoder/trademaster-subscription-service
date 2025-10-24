package com.trademaster.subscription.constants;

import java.math.BigDecimal;

/**
 * Pricing Constants
 * MANDATORY: Rule #17 - Constants & Magic Numbers
 *
 * Centralized pricing configuration for subscription tiers.
 * All pricing values in USD.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class PricingConstants {

    // Monthly Subscription Pricing (USD)
    public static final BigDecimal PRO_MONTHLY_PRICE = new BigDecimal("29.99");
    public static final BigDecimal AI_PREMIUM_MONTHLY_PRICE = new BigDecimal("99.99");
    public static final BigDecimal INSTITUTIONAL_MONTHLY_PRICE = new BigDecimal("299.99");

    // Quarterly Subscription Pricing (USD)
    public static final BigDecimal PRO_QUARTERLY_PRICE = new BigDecimal("79.99");
    public static final BigDecimal AI_PREMIUM_QUARTERLY_PRICE = new BigDecimal("269.99");
    public static final BigDecimal INSTITUTIONAL_QUARTERLY_PRICE = new BigDecimal("809.99");

    // Annual Subscription Pricing (USD)
    public static final BigDecimal PRO_ANNUAL_PRICE = new BigDecimal("299.99");
    public static final BigDecimal AI_PREMIUM_ANNUAL_PRICE = new BigDecimal("999.99");
    public static final BigDecimal INSTITUTIONAL_ANNUAL_PRICE = new BigDecimal("2999.99");

    // Promotional Discount
    public static final BigDecimal PROMOTIONAL_DISCOUNT_PERCENT = new BigDecimal("0.20"); // 20%

    private PricingConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
