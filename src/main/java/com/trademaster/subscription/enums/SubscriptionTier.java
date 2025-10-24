package com.trademaster.subscription.enums;

import com.trademaster.subscription.config.SubscriptionTierConfig;
import com.trademaster.subscription.constants.FeatureNameConstants;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Subscription Tier Enumeration
 * MANDATORY: Rule #16 - Dynamic Configuration (ALL values externalized)
 *
 * Defines the different subscription tiers available in TradeMaster.
 * All pricing, features, and limits are loaded from application.yml.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Getter
public enum SubscriptionTier {

    FREE,
    PRO,
    AI_PREMIUM,
    INSTITUTIONAL;

    /**
     * Static configuration holder - initialized by SubscriptionTierConfigInitializer
     * MANDATORY: Rule #16 - All configuration externalized to application.yml
     */
    private static Map<SubscriptionTier, SubscriptionTierConfig.TierConfig> configMap;

    /**
     * Initialize configuration from Spring-managed SubscriptionTierConfig
     * MANDATORY: Called by SubscriptionTierConfigInitializer @PostConstruct
     */
    public static void initializeConfiguration(SubscriptionTierConfig tierConfig) {
        configMap = new EnumMap<>(SubscriptionTier.class);

        configMap.put(FREE, tierConfig.getTierConfig("free"));
        configMap.put(PRO, tierConfig.getTierConfig("pro"));
        configMap.put(AI_PREMIUM, tierConfig.getTierConfig("ai_premium"));
        configMap.put(INSTITUTIONAL, tierConfig.getTierConfig("institutional"));
    }

    /**
     * Get tier configuration
     * MANDATORY: Rule #16 - All values from externalized config
     */
    private SubscriptionTierConfig.TierConfig getConfig() {
        return java.util.Optional.ofNullable(configMap)
            .map(map -> map.get(this))
            .orElseThrow(() -> new IllegalStateException(
                "Tier configuration not initialized. Ensure SubscriptionTierConfigInitializer ran."
            ));
    }

    /**
     * Get display name for this tier
     */
    public String getDisplayName() {
        return getConfig().getDisplayName();
    }

    /**
     * Get description for this tier
     */
    public String getDescription() {
        return getConfig().getDescription();
    }

    /**
     * Get monthly price for this tier
     */
    public BigDecimal getMonthlyPrice() {
        return getConfig().getPricing().getMonthly();
    }

    /**
     * Get quarterly price for this tier
     */
    public BigDecimal getQuarterlyPrice() {
        return getConfig().getPricing().getQuarterly();
    }

    /**
     * Get annual price for this tier
     */
    public BigDecimal getAnnualPrice() {
        return getConfig().getPricing().getAnnual();
    }

    /**
     * Get features available for this tier
     */
    public List<String> getFeatures() {
        return getConfig().getFeatures();
    }

    /**
     * Get usage limits for this tier
     */
    public SubscriptionLimits getLimits() {
        SubscriptionTierConfig.LimitsConfig limits = getConfig().getLimits();

        return SubscriptionLimits.builder()
            .maxWatchlists(limits.getMaxWatchlists())
            .maxAlerts(limits.getMaxAlerts())
            .apiCallsPerDay(limits.getApiCallsPerDay())
            .maxPortfolios(limits.getMaxPortfolios())
            .aiAnalysisPerMonth(limits.getAiAnalysisPerMonth())
            .maxSubAccounts(limits.getMaxSubAccounts())
            .maxCustomIndicators(limits.getMaxCustomIndicators())
            .dataRetentionDays(limits.getDataRetentionDays())
            .maxWebSocketConnections(limits.getMaxWebSocketConnections())
            .build();
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
     * MANDATORY: Rule #3 - No if-else, using pattern matching
     */
    public boolean hasUnlimitedUsage(String feature) {
        return switch (feature.toLowerCase()) {
            case FeatureNameConstants.WATCHLISTS -> getLimits().getMaxWatchlists() == -1;
            case FeatureNameConstants.ALERTS -> getLimits().getMaxAlerts() == -1;
            case FeatureNameConstants.API_CALLS -> getLimits().getApiCallsPerDay() == -1;
            case FeatureNameConstants.PORTFOLIOS -> getLimits().getMaxPortfolios() == -1;
            case "ai_analysis" -> getLimits().getAiAnalysisPerMonth() == -1;
            default -> false;
        };
    }

    /**
     * Get usage limit for a specific feature
     * MANDATORY: Rule #3 - No if-else, using pattern matching
     */
    public Long getUsageLimit(String feature) {
        return switch (feature.toLowerCase()) {
            case FeatureNameConstants.WATCHLISTS -> Long.valueOf(getLimits().getMaxWatchlists());
            case FeatureNameConstants.ALERTS -> Long.valueOf(getLimits().getMaxAlerts());
            case FeatureNameConstants.API_CALLS -> Long.valueOf(getLimits().getApiCallsPerDay());
            case FeatureNameConstants.PORTFOLIOS -> Long.valueOf(getLimits().getMaxPortfolios());
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
