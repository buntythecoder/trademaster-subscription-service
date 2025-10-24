package com.trademaster.subscription.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Notification Event Record
 * MANDATORY: Immutability - TradeMaster Rule #9 (Records for DTOs)
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 *
 * Basic subscription event for notification processing.
 *
 * @author TradeMaster Development Team
 */
public record SubscriptionNotificationEvent(
    UUID subscriptionId,
    UUID userId,
    SubscriptionEventType eventType,
    LocalDateTime timestamp,
    String correlationId
) {}
