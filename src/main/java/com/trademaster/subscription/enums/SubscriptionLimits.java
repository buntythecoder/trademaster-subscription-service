package com.trademaster.subscription.enums;

import lombok.Builder;
import lombok.Data;

/**
 * Subscription Usage Limits
 * 
 * Defines the usage limits for each subscription tier.
 * -1 indicates unlimited usage.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class SubscriptionLimits {
    
    /**
     * Maximum number of watchlists (-1 for unlimited)
     */
    private final int maxWatchlists;
    
    /**
     * Maximum number of alerts (-1 for unlimited)
     */
    private final int maxAlerts;
    
    /**
     * Maximum API calls per day (-1 for unlimited)
     */
    private final int apiCallsPerDay;
    
    /**
     * Maximum number of portfolios (-1 for unlimited)
     */
    private final int maxPortfolios;
    
    /**
     * Maximum AI analysis requests per month (-1 for unlimited)
     */
    @Builder.Default
    private final int aiAnalysisPerMonth = 0;
    
    /**
     * Maximum number of sub-accounts for institutional tier (-1 for unlimited)
     */
    @Builder.Default
    private final int maxSubAccounts = 0;
    
    /**
     * Maximum number of custom indicators
     */
    @Builder.Default
    private final int maxCustomIndicators = 0;
    
    /**
     * Maximum data retention period in days (-1 for unlimited)
     */
    @Builder.Default
    private final int dataRetentionDays = 30;
    
    /**
     * Maximum concurrent WebSocket connections
     */
    @Builder.Default
    private final int maxWebSocketConnections = 1;
    
    /**
     * Check if a feature has unlimited usage
     */
    public boolean isUnlimited(String feature) {
        return switch (feature.toLowerCase()) {
            case "watchlists" -> maxWatchlists == -1;
            case "alerts" -> maxAlerts == -1;
            case "api_calls" -> apiCallsPerDay == -1;
            case "portfolios" -> maxPortfolios == -1;
            case "ai_analysis" -> aiAnalysisPerMonth == -1;
            case "sub_accounts" -> maxSubAccounts == -1;
            case "custom_indicators" -> maxCustomIndicators == -1;
            case "data_retention" -> dataRetentionDays == -1;
            default -> false;
        };
    }
    
    /**
     * Get the limit value for a specific feature
     */
    public int getLimitValue(String feature) {
        return switch (feature.toLowerCase()) {
            case "watchlists" -> maxWatchlists;
            case "alerts" -> maxAlerts;
            case "api_calls" -> apiCallsPerDay;
            case "portfolios" -> maxPortfolios;
            case "ai_analysis" -> aiAnalysisPerMonth;
            case "sub_accounts" -> maxSubAccounts;
            case "custom_indicators" -> maxCustomIndicators;
            case "data_retention" -> dataRetentionDays;
            case "websocket_connections" -> maxWebSocketConnections;
            default -> 0;
        };
    }
}