package com.trademaster.subscription.enums;

import com.trademaster.subscription.config.SubscriptionTierConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Subscription Tier Configuration Initializer
 * MANDATORY: Rule #16 - Dynamic Configuration initialization
 *
 * Initializes the SubscriptionTier enum with externalized configuration
 * from application.yml using a static holder pattern.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionTierConfigInitializer {

    private final SubscriptionTierConfig tierConfig;

    /**
     * Initialize SubscriptionTier enum with externalized configuration
     * MANDATORY: Called after Spring context initialization
     */
    @PostConstruct
    public void initializeTierConfiguration() {
        log.info("Initializing subscription tier configuration from application.yml");

        SubscriptionTier.initializeConfiguration(tierConfig);

        log.info("Successfully initialized {} subscription tiers",
            SubscriptionTier.values().length);
    }
}
