package com.trademaster.subscription.constants;

/**
 * Feature Name Constants
 * MANDATORY: Rule #17 - All feature identifiers centralized
 * MANDATORY: Rule #16 - Dynamic Configuration (externalized values)
 *
 * Defines all subscription feature identifiers used throughout the system.
 * Ensures consistency across usage tracking, limit enforcement, and tier configuration.
 *
 * @author TradeMaster Development Team
 */
public final class FeatureNameConstants {

    private FeatureNameConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Core Features
    public static final String API_CALLS = "api_calls";
    public static final String PORTFOLIOS = "portfolios";
    public static final String WATCHLISTS = "watchlists";
    public static final String ALERTS = "alerts";
    public static final String AI_INSIGHTS = "ai_insights";

    // Advanced Features (Enterprise tier)
    public static final String ADVANCED_ANALYTICS = "advanced_analytics";
    public static final String CUSTOM_STRATEGIES = "custom_strategies";
    public static final String PRIORITY_SUPPORT = "priority_support";
    public static final String API_WEBHOOKS = "api_webhooks";
    public static final String REAL_TIME_DATA = "real_time_data";
}
