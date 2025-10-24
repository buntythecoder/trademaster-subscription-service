package com.trademaster.subscription.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Billing Notification Event Record
 * MANDATORY: Immutability - TradeMaster Rule #9 (Records for DTOs)
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 *
 * Event for billing-related notifications with transaction information.
 *
 * @author TradeMaster Development Team
 */
public record SubscriptionBillingNotificationEvent(
    UUID subscriptionId,
    UUID userId,
    UUID transactionId,
    BigDecimal billingAmount,
    LocalDateTime timestamp,
    String correlationId
) {}
