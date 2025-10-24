package com.trademaster.subscription.constants;

/**
 * Usage Limit Constants
 * MANDATORY: Rule #17 - Constants & Magic Numbers
 *
 * Centralized usage limits for subscription features and resources.
 * Values represent maximum allowed usage before throttling or blocking.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class UsageLimitConstants {

    // API Call Limits (per day)
    public static final int FREE_TIER_API_CALLS_PER_DAY = 1000;
    public static final int PRO_TIER_API_CALLS_PER_DAY = 10000;
    public static final int AI_PREMIUM_API_CALLS_PER_DAY = 50000;
    public static final int INSTITUTIONAL_API_CALLS_PER_DAY = -1; // Unlimited

    // Portfolio Limits
    public static final int FREE_TIER_MAX_PORTFOLIOS = 3;
    public static final int PRO_TIER_MAX_PORTFOLIOS = 10;
    public static final int AI_PREMIUM_MAX_PORTFOLIOS = 50;
    public static final int INSTITUTIONAL_MAX_PORTFOLIOS = -1; // Unlimited

    // Agent Limits
    public static final int FREE_TIER_MAX_AI_AGENTS = 0;
    public static final int PRO_TIER_MAX_AI_AGENTS = 0;
    public static final int AI_PREMIUM_MAX_AI_AGENTS = 5;
    public static final int INSTITUTIONAL_MAX_AI_AGENTS = -1; // Unlimited

    // Session Limits
    public static final int FREE_TIER_MAX_CONCURRENT_SESSIONS = 1;
    public static final int PRO_TIER_MAX_CONCURRENT_SESSIONS = 3;
    public static final int AI_PREMIUM_MAX_CONCURRENT_SESSIONS = 10;
    public static final int INSTITUTIONAL_MAX_CONCURRENT_SESSIONS = -1; // Unlimited

    // Unlimited Indicator
    public static final int UNLIMITED = -1;

    private UsageLimitConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
