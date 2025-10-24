package com.trademaster.subscription.events;

import com.trademaster.subscription.enums.SubscriptionTier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Upgrade Notification Event Record
 * MANDATORY: Immutability - TradeMaster Rule #9 (Records for DTOs)
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 *
 * Event for subscription tier upgrades with previous tier information.
 *
 * @author TradeMaster Development Team
 */
public record SubscriptionUpgradeNotificationEvent(
    UUID subscriptionId,
    UUID userId,
    SubscriptionTier previousTier,
    SubscriptionTier newTier,
    LocalDateTime timestamp,
    String correlationId
) {}
