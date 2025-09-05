package com.trademaster.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Subscription Tier Enumeration
 * 
 * Defines the different subscription tiers available in TradeMaster
 * with their pricing, features, and usage limits.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    
    FREE("Free", BigDecimal.ZERO, "Basic market data access") {
        @Override
        public List<String> getFeatures() {
            return List.of(
                "Basic market data",
                "5 watchlists",
                "Basic charts",
                "Community support"
            );
        }
        
        @Override
        public SubscriptionLimits getLimits() {
            return SubscriptionLimits.builder()
                .maxWatchlists(5)
                .maxAlerts(10)
                .apiCallsPerDay(100)
                .maxPortfolios(1)
                .build();
        }
    },
    
    PRO("Pro", new BigDecimal("999"), "Real-time data and advanced analytics") {
        @Override
        public List<String> getFeatures() {
            return List.of(
                "Real-time market data",
                "Unlimited watchlists",
                "Advanced charts with 50+ indicators",
                "Portfolio analytics",
                "Basic alerts and notifications",
                "Export capabilities",
                "Priority email support"
            );
        }
        
        @Override
        public SubscriptionLimits getLimits() {
            return SubscriptionLimits.builder()
                .maxWatchlists(-1) // Unlimited
                .maxAlerts(100)
                .apiCallsPerDay(10000)
                .maxPortfolios(5)
                .build();
        }
    },
    
    AI_PREMIUM("AI Premium", new BigDecimal("2999"), "AI-powered trading insights and behavioral analytics") {
        @Override
        public List<String> getFeatures() {
            return List.of(
                "All Pro features",
                "Behavioral AI insights",
                "Trading psychology analytics",
                "Emotion tracking and coaching",
                "AI-powered recommendations",
                "Advanced risk management",
                "Backtesting capabilities",
                "Phone and chat support"
            );
        }
        
        @Override
        public SubscriptionLimits getLimits() {
            return SubscriptionLimits.builder()
                .maxWatchlists(-1)
                .maxAlerts(500)
                .apiCallsPerDay(50000)
                .maxPortfolios(20)
                .aiAnalysisPerMonth(1000)
                .build();
        }
    },
    
    INSTITUTIONAL("Institutional", new BigDecimal("25000"), "Enterprise-grade features for institutions") {
        @Override
        public List<String> getFeatures() {
            return List.of(
                "All AI Premium features",
                "Multi-user account management",
                "Custom API integrations",
                "Advanced analytics suite",
                "Risk management tools",
                "Compliance reporting",
                "White-label options",
                "Dedicated account manager",
                "24/7 priority support"
            );
        }
        
        @Override
        public SubscriptionLimits getLimits() {
            return SubscriptionLimits.builder()
                .maxWatchlists(-1)
                .maxAlerts(-1)
                .apiCallsPerDay(-1) // Unlimited
                .maxPortfolios(-1)
                .aiAnalysisPerMonth(-1)
                .maxSubAccounts(100)
                .build();
        }
    };
    
    private final String displayName;
    private final BigDecimal monthlyPrice;
    private final String description;
    
    /**
     * Get the features available for this subscription tier
     */
    public abstract List<String> getFeatures();
    
    /**
     * Get the usage limits for this subscription tier
     */
    public abstract SubscriptionLimits getLimits();
    
    /**
     * Get quarterly price with discount
     */
    public BigDecimal getQuarterlyPrice() {
        return switch (this) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> new BigDecimal("2799"); // 6% discount
            case AI_PREMIUM -> new BigDecimal("8099"); // 10% discount
            case INSTITUTIONAL -> new BigDecimal("67500"); // 10% discount
        };
    }
    
    /**
     * Get annual price with discount
     */
    public BigDecimal getAnnualPrice() {
        return switch (this) {
            case FREE -> BigDecimal.ZERO;
            case PRO -> new BigDecimal("9999"); // 17% discount
            case AI_PREMIUM -> new BigDecimal("29999"); // 17% discount
            case INSTITUTIONAL -> new BigDecimal("250000"); // 17% discount
        };
    }
    
    /**
     * Check if this tier has a specific feature
     */
    public boolean hasFeature(String feature) {
        return getFeatures().stream()
            .anyMatch(f -> f.toLowerCase().contains(feature.toLowerCase()));
    }
    
    /**
     * Check if this tier supports unlimited usage for a specific feature
     */
    public boolean hasUnlimitedUsage(String feature) {
        return switch (feature.toLowerCase()) {
            case "watchlists" -> getLimits().getMaxWatchlists() == -1;
            case "alerts" -> getLimits().getMaxAlerts() == -1;
            case "api_calls" -> getLimits().getApiCallsPerDay() == -1;
            case "portfolios" -> getLimits().getMaxPortfolios() == -1;
            case "ai_analysis" -> getLimits().getAiAnalysisPerMonth() == -1;
            default -> false;
        };
    }
    
    /**
     * Get usage limit for a specific feature
     */
    public Long getUsageLimit(String feature) {
        return switch (feature.toLowerCase()) {
            case "watchlists" -> Long.valueOf(getLimits().getMaxWatchlists());
            case "alerts" -> Long.valueOf(getLimits().getMaxAlerts());
            case "api_calls" -> Long.valueOf(getLimits().getApiCallsPerDay());
            case "portfolios" -> Long.valueOf(getLimits().getMaxPortfolios());
            case "ai_analysis" -> Long.valueOf(getLimits().getAiAnalysisPerMonth());
            default -> 0L;
        };
    }
    
    /**
     * Check if this tier can be upgraded to another tier
     */
    public boolean canUpgradeTo(SubscriptionTier targetTier) {
        return this.ordinal() < targetTier.ordinal();
    }
    
    /**
     * Check if this tier can be downgraded to another tier
     */
    public boolean canDowngradeTo(SubscriptionTier targetTier) {
        return this.ordinal() > targetTier.ordinal();
    }
    
    /**
     * Get the next higher tier
     */
    public SubscriptionTier getNextTier() {
        SubscriptionTier[] tiers = values();
        int currentIndex = this.ordinal();
        return currentIndex < tiers.length - 1 ? tiers[currentIndex + 1] : this;
    }
    
    /**
     * Get the previous lower tier
     */
    public SubscriptionTier getPreviousTier() {
        SubscriptionTier[] tiers = values();
        int currentIndex = this.ordinal();
        return currentIndex > 0 ? tiers[currentIndex - 1] : this;
    }
}