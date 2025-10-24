package com.trademaster.subscription.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Cancellation Notification Event Record
 * MANDATORY: Immutability - TradeMaster Rule #9 (Records for DTOs)
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 *
 * Event for subscription cancellations with cancellation reason.
 *
 * @author TradeMaster Development Team
 */
public record SubscriptionCancellationNotificationEvent(
    UUID subscriptionId,
    UUID userId,
    String cancellationReason,
    LocalDateTime timestamp,
    String correlationId
) {}
