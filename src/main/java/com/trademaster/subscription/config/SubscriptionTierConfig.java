package com.trademaster.subscription.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Subscription Tier Configuration
 * MANDATORY: Rule #16 - Dynamic Configuration (ALL configuration externalized)
 * MANDATORY: Rule #9 - Immutable Data (Data class with validation)
 *
 * Externalizes subscription tier pricing, limits, and features to application.yml.
 * Enables dynamic pricing changes without code deployment.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "trademaster.subscription.tiers")
@Validated
@Data
public class SubscriptionTierConfig {

    @NotEmpty(message = "At least one subscription tier must be configured")
    private Map<String, TierConfig> tiers;

    /**
     * Configuration for a single subscription tier
     */
    @Data
    public static class TierConfig {

        @NotNull(message = "Display name is required")
        private String displayName;

        @NotNull(message = "Description is required")
        private String description;

        @NotNull(message = "Pricing configuration is required")
        @Valid
        private PricingConfig pricing;

        @NotNull(message = "Limits configuration is required")
        @Valid
        private LimitsConfig limits;

        @NotEmpty(message = "At least one feature must be defined")
        private List<String> features;
    }

    /**
     * Pricing configuration for subscription tiers
     */
    @Data
    public static class PricingConfig {

        @NotNull(message = "Monthly price is required")
        private BigDecimal monthly;

        @NotNull(message = "Quarterly price is required")
        private BigDecimal quarterly;

        @NotNull(message = "Annual price is required")
        private BigDecimal annual;
    }

    /**
     * Usage limits configuration for subscription tiers
     * MANDATORY: -1 = unlimited usage
     */
    @Data
    public static class LimitsConfig {

        private int maxWatchlists = -1;          // -1 = unlimited
        private int maxAlerts = -1;              // -1 = unlimited
        private int apiCallsPerDay = -1;         // -1 = unlimited
        private int maxPortfolios = -1;          // -1 = unlimited
        private int aiAnalysisPerMonth = 0;      // 0 = not available
        private int maxSubAccounts = 0;          // 0 = not available
        private int maxCustomIndicators = 0;     // 0 = not available
        private int dataRetentionDays = 30;      // Default: 30 days
        private int maxWebSocketConnections = 1; // Default: 1 connection
    }

    /**
     * Get tier configuration by tier name
     * MANDATORY: Rule #3 - No if-else, using functional pattern
     */
    public TierConfig getTierConfig(String tierName) {
        return java.util.Optional.ofNullable(tiers.get(tierName.toLowerCase()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Tier configuration not found for: " + tierName
            ));
    }

    /**
     * Check if tier exists in configuration
     */
    public boolean hasTier(String tierName) {
        return tiers.containsKey(tierName.toLowerCase());
    }
}
